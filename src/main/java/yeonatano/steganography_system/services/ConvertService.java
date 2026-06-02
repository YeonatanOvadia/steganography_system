package yeonatano.steganography_system.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * שירות המרת קבצים (File Conversion Service) המשתמש ב-API של CloudConvert.
 * שירות זה נועד לפתור את בעיית חוסר התאימות (Incompatibility) של פורמטים מסוימים לאלגוריתמי הסטגנוגרפיה.
 * * 💡 דגש ארכיטקטוני: השירות מתוכנן לעבוד במודל In-Memory בלבד.
 * כלומר, הוא מקבל, משדר, ומחזיר מערכי בייטים (byte[]) מבלי לכתוב או לקרוא קבצים פיזיים (I/O) 
 * מהדיסק הקשיח של השרת. זה מונע יצירת "קובצי זבל" ומשפר משמעותית את זמן התגובה ואבטחת המידע.
 */
// @Service מסמן ל-Spring לנהל את המחלקה הזו כמופע יחיד (Singleton) ולהזריק אותה איפה שצריך.
@Service
public class ConvertService 
{

    // מפתח אימות (Bearer Token) להתחברות מאובטחת מול שרתי CloudConvert.
    // הערה להגנה: בפרודקשן אמיתי מפתח כזה נשמר כמשתנה סביבה (Environment Variable) או ב-application.properties.
    // למה? כדי למנוע קידוד קשיח (Hardcoding) של סודות בקוד המקור שעלול לדלוף.
    @Value("${cloudconvert.api.key}")
    private String API_KEY;

    // קליינט HTTP סינכרוני מבית Square לביצוע בקשות רשת יעילות
    // למה OkHttpClient? כי הוא מנהל "בריכת חיבורים" (Connection Pool) אוטומטית ויעיל מאוד עם קבצים בינאריים.
    private OkHttpClient client;

    /**
     * בנאי המחלקה - מאתחל את ה-OkHttpClient עם הגדרות Timeout מותאמות אישית.
     */
    public ConvertService() 
    {
        // המרת קבצים (במיוחד קבצי שמע ווידאו) היא פעולה איטית התלויה בשרת צד-שלישי.
        // הגדלת זמן ההמתנה (Timeout) ל-60 שניות מונעת מהאפליקציה לקרוס או לזרוק שגיאת 
        // SocketTimeoutException בטרם ההמרה הושלמה. ברירת המחדל (לרוב 10 שניות) קצרה מדי פה.
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * פונקציית העל (Orchestrator) המנהלת את כל מחזור החיים של תהליך ההמרה.
     *
     * @param fileData הנתונים הבינאריים של הקובץ המקורי להמרה.
     * @param sourceMimeType סוג הקובץ המקורי (למשל "image/webp") כדי שהשרת ידע לפענח אותו.
     * @param targetFormat פורמט היעד המבוקש (למשל "png" או "wav").
     * @return מערך בייטים המייצג את הקובץ לאחר המרה.
     * @throws Exception זורק חריגה במקרה של כשל תקשורת או שגיאת המרה בשרת.
     */
    public byte[] convertFormat(byte[] fileData, String sourceMimeType, String targetFormat) throws Exception 
    {
        // חילוץ סיומת הקובץ מתוך ה-MIME Type (למשל מ-"image/webp" נחלץ "webp").
        // למה זה הכרחי? ה-API של CloudConvert מקבל זרם ביטים, והוא חייב סיומת כדי לדעת
        // איזה אלגוריתם (Codec) להפעיל כדי לקרוא את קובץ המקור כראוי.
        String sourceExt = "bin"; // Fallback ליתר ביטחון
        if (sourceMimeType != null && sourceMimeType.contains("/")) 
        {
            sourceExt = sourceMimeType.substring(sourceMimeType.indexOf("/") + 1);
            // אני בעצם רוצה לדעת איזה סוג קובץ זה ולהמיר את הפורמט הזה
        }

        // שלב 1: הצהרת כוונות (Job Creation)
        // פנייה לשרת ליצירת מסגרת עבודה הכוללת העלאה, המרה לפורמט היעד, ויצירת קישור ייצוא.
        System.out.println("[CloudConvert] שלב 1: יצירת Job להמרה מ-" + sourceExt + " ל-" + targetFormat + "...");

        // הכנת תוכנית עבודה עבור הקובץ וקבלת קישור להעלאה
        JSONObject jobResponse = createJob(targetFormat);

        //חילוץ ID של פעולת ההמרה
        String jobId = jobResponse.getJSONObject("data").getString("id");

        // חילוץ המשמה מתגובת השרת
        JSONObject uploadTask = jobResponse.getJSONObject("data").getJSONArray("tasks").getJSONObject(0);


        // שלב 2: העלאת המידע הבינארי לכתובת הייעודית שקיבלנו מהשרת
        System.out.println("[CloudConvert] שלב 2: העלאת הקובץ לשרת...");
        // צירוף חלקי המשימה ביחד ושליחתם לשרת
        uploadFile(uploadTask, fileData, sourceMimeType, sourceExt);

        // שלב 3: המתנה פעילה (Polling) עד לסיום העיבוד בצד השרת
        // נשאל את השרת כל כמה זמן האם הוא סיים
        //כשיסיים נחלץ את הקישור של ההורדה מהמשימה 
        System.out.println("[CloudConvert] שלב 3: המתנה לסיום ההמרה...");
        String downloadUrl = waitForResult(jobId);

        // שלב 4: משיכת התוצאה המוצלחת ישירות לתוך זיכרון ה-RAM
        // ניכנס לקישור ההורדה ונחלץ מהתגובה את המטא דאטה של הקובץ ונחזיר את הכל במערך בתים למי שזימן את הפונקצייה
        System.out.println("[CloudConvert] שלב 4: הורדת הקובץ המומר...");

        return downloadFileAsBytes(downloadUrl);
    }

    /**
     * פונה ל-API של CloudConvert כדי להגדיר את משימת ההמרה (Job).
     * מגדירה צינור נתונים (Pipeline) בעל 3 שלבים: Import -> Convert -> Export.
     */
    private JSONObject createJob(String targetFormat) throws IOException 
    {
        // שימוש בתכונת Text Blocks ליצירת JSON קריא ונוח לתחזוקה.
        // למה? זה חוסך את הצורך לשרשר מחרוזות (+) ולהשתמש בתווי מילוט (\") שהופכים את הקוד למבולגן.
        // הזרקת פורמט היעד (%s) מתבצעת באופן דינמי באמצעות פונקציית formatted.
        String jsonPayload = """
            {
                "tasks": {
                    "import-1": {
                        "operation": "import/upload"
                    },
                    "task-1": {
                        "operation": "convert",
                        "input": "import-1",
                        "output_format": "%s"
                    },
                    "export-1": {
                        "operation": "export/url",
                        "input": "task-1",
                        "inline": false,
                        "archive_multiple_files": false
                    }
                },
                "tag": "jobbuilder"
            }
            """.formatted(targetFormat);

        // בניית בקשת HTTP POST עם JSON Payload וסוג התוכן המתאים
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://api.cloudconvert.com/v2/jobs")
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        // למה "try(Response...)"? זה נקרא Try-with-resources. 
        // זה מבטיח שהחיבור לרשת (ה-Socket) ייסגר אוטומטית בסוף הבלוק, ומונע זליגת זיכרון/משאבים (Resource Leak).
        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful()) 
                throw new IOException("Failed to create job: " + response.body().string());
            
            return new JSONObject(response.body().string());
        }
    }

    /**
     * מטפלת בתהליך ההעלאה של הקובץ הגולמי באמצעות Multipart Form Data.
     */
    private void uploadFile(JSONObject uploadTask, byte[] fileData, String mimeType, String ext) throws IOException 
    {
        // חילוץ פרטי הטופס (כתובת ופרמטרי אימות) המאפשרים העלאה ישירה לשרת אחסון הזמני
        JSONObject form = uploadTask.getJSONObject("result").getJSONObject("form");
        String uploadUrl = form.getString("url");
        JSONObject parameters = form.getJSONObject("parameters");

        // למה MultipartBody? פרוטוקול HTTP מבוסס טקסט. התקן הזה מאפשר לשלוח גם טקסט (הפרמטרים) 
        // וגם קובץ בינארי (הבייטים) באותה בקשה, כשהוא מייצר מחיצות/גבולות (Boundaries) ביניהם.
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        
        // הזרקת כל פרמטרי ההרשאה שחזרו מהשרת לתוך הטופס
        // כדי ששרת ההמרה ידע לשלוף משרת הנתונים את הקובץ שעלה
        for (String key : parameters.keySet())
            builder.addFormDataPart(key, parameters.get(key).toString());
        
        // הזרקת הקובץ עצמו (fileData) בליווי שם קובץ וירטואלי וה-MIME Type שלו.
        // הגדרת סיומת נכונה (ext) היא קריטית לפענוח מוצלח על ידי המנוע של CloudConvert.
        // application/octet-stream אומר לשרת: "זהו זרם של מידע בינארי גולמי".
        String filename = "upload_file." + ext;
        builder.addFormDataPart("file", filename,
                RequestBody.create(fileData, MediaType.parse(mimeType != null ? mimeType : "application/octet-stream")));

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(builder.build())
                .build();

        try (Response response = client.newCall(request).execute()) 
        {
            if (!response.isSuccessful()) 
                throw new IOException("Upload failed: " + response.code());
        }
    }

    /**
     * פונקציית דגימה מחזורית (Polling).
     * מכיוון ש-CloudConvert מבצע פעולות אסינכרוניות אצלו בשרת, אנו נדרשים לבדוק את
     * מצב המשימה (Status) כל מספר שניות עד שהיא מסתיימת (או נכשלת).
     */
    private String waitForResult(String jobId) throws Exception 
    {
        while (true) // לולאה הרצה עד לקבלת תוצאה או זריקת שגיאה (יישבר באמצעות return או throw)
        {
            Request request = new Request.Builder()
                    .url("https://api.cloudconvert.com/v2/jobs/" + jobId)
                    .header("Authorization", "Bearer " + API_KEY)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) 
            {
                if (!response.isSuccessful()) 
                    throw new IOException("Failed to check status");

                // המרת התשובה הטקסטואלית לאובייקט JSON כדי שנוכל לשלוף ממנו נתונים בקלות
                JSONObject json = new JSONObject(response.body().string());

                // נחלץ את הסטטוס מהתגובה של השרת
                String status = json.getJSONObject("data").getString("status");

                if (status.equals("finished")) 
                {
                    // חיפוש הקישור (URL) המיוצא מתוך אובייקט ה-JSON המסועף
                    // למה הלולאה? כי JSON מחזיר עץ נתונים, ויכולות להיות כמה משימות (tasks). אנחנו מחפשים רק את שלב הייצוא.
                    JSONArray tasks = json.getJSONObject("data").getJSONArray("tasks");
                    // נעבור על רשימת המשימות שקיבלנו 
                    // נחפש את משימת הייצוא
                    for (int i = 0; i < tasks.length(); i++) 
                    {
                        JSONObject task = tasks.getJSONObject(i);
                        if (task.getString("operation").equals("export/url"))
                        {
                            return task.getJSONObject("result").getJSONArray("files").getJSONObject(0).getString("url");
                        }
                    }
                } 
                else if (status.equals("error")) 
                {
                    throw new Exception("ההמרה נכשלה בשרת CloudConvert.");
                }
            
                // למה חובה להרדים (Sleep)? 
                // אם נשאל את השרת עשרות פעמים בשנייה אם הוא סיים, ניחסם על ידי השרת (Rate Limiting) 
                // או ניחשב כמתקפת מניעת שירות (DoS). השהייה של 3 שניות היא מרווח הגיוני וסביר.
                Thread.sleep(3000); 
            }
        }
    }

    /**
     * ביצוע קריאת HTTP GET לכתובת שהתקבלה, ומשיכת כל גוף התשובה למערך בייטים (In-Memory).
     */
    private byte[] downloadFileAsBytes(String downloadUrl) throws IOException 
    {
        Request request = new Request.Builder().url(downloadUrl).get().build();
        try (Response response = client.newCall(request).execute()) 
        {
            if (!response.isSuccessful() || response.body() == null) 
            {
                throw new IOException("הורדת הקובץ נכשלה");
            }
            // למה .bytes() כל כך קריטי פה? 
            // זה קורא את כל המידע שמגיע מהרשת במכה אחת ישירות לתוך זיכרון ה-RAM (כמערך בייטים).
            // זה מונע את הצורך ליצור קובץ זמני על הדיסק, מה שמשאיר אפס עקבות במערכת הפעלה 
            // ומשפר את אבטחת המידע במערכת הסטגנוגרפיה שלך.
            return response.body().bytes();
        }
    }
}