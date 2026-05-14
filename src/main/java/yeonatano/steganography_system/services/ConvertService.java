package yeonatano.steganography_system.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * שירות המרת קבצים המשתמש ב-API של CloudConvert.
 * השירות מבצע הכל בזיכרון (In-Memory) בעזרת מערכי בייטים, ללא שמירת קבצים פיזיים על השרת,
 * מה שמשפר משמעותית את ביצועי המערכת ומונע תקלות תשתית.
 */
@Service
public class ConvertService 
{

    // מפתח ה-API שלך (הערה: בסביבת ייצור אמיתית, מומלץ להעביר את זה לקובץ application.properties)
    private static final String API_KEY = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiMGE1Y2IxMzg4NDI4ZmM5MzhjZjk0ZWE0NGY5ODc3ODI2OWEwZDBlNDZjMGE5OTFiMDZjZTcyODhiZGRjYzFiMWM5ZDYyOTQ4N2MwYTE5MmYiLCJpYXQiOjE3Nzg3NzYxNTAuNzIwMjg5LCJuYmYiOjE3Nzg3NzYxNTAuNzIwMjksImV4cCI6NDkzNDQ0OTc1MC43MTQxNzIsInN1YiI6Ijc1NTU1OTYyIiwic2NvcGVzIjpbInVzZXIud3JpdGUiLCJ1c2VyLnJlYWQiLCJ0YXNrLnJlYWQiLCJ0YXNrLndyaXRlIiwid2ViaG9vay5yZWFkIiwid2ViaG9vay53cml0ZSIsInByZXNldC5yZWFkIiwicHJlc2V0LndyaXRlIl19.nSPmVqgbisqVbSr6y_546Cvm16W2eEz3ngY6PK_WhYTEHpXnUk5rttrxHFI2fWn3vUwI1YyP9tABeXJe0oFXot-PYWSMr7SwK3OndtRzHrhYzvhG0sydcA0Xsv83oecsa87eF3GOTY-iXmWRJ-JzHNn1QtNPyrq13Ie1Eb1BKoH5KcvaPZuwycHUVMSomm_xV2N5-Z3PNnVq9qKnAfWKH3fCJkn2w6UQUd0GqHGDZJETpCJT-xqW6bja5BHvKWomaFH4nhNZFSve1Vop2AAxNmaRlVhTDnBBsvUymdfSC0hOKJvoAOoMY6y_pNskZDYyBFBRRCfa2jwiezWW1R4v3dM172Y0JpMDkSvd9lymqEUvT-6mxd7TT7sWEvNiUDUNxmwbei3PtgJ8WxGfUXQzsZhlxlvBJKorK68s9hYJf9Ojk6B8mqmj7dEFc2Wb8HefI1Q4mfl4HdmfiZiyJZUwpzo4HIUO1fgTJ9dEtQjiWXL3ortjgbkVGkqXvmjit-XT3fQueT2I9WGEnKhg18n5T-hxLcC6EejgI73g3FWqv01SLKbNIF-TWCLzsj1VYchO10Hn0NsPlR-kNh-B7K2EAps8RkCCPzQIBggPBGpkI6aafbapQG89qOJOuw4FUejUS2oLlxmJiz0SG2XCtUFcQquwV7kNqwMRTDAePnVnbLU"; 

    private final OkHttpClient client;

    public ConvertService() 
    {
        // זמן המתנה מוגדל (60 שניות) כי המרות יכולות לקחת זמן
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * @param fileData הנתונים הבינאריים של הקובץ המקורי.
     * @param sourceMimeType סוג הקובץ המקורי (למשל "image/webp" או "audio/mpeg") - קריטי כדי שהשרת יזהה!
     * @param targetFormat הפורמט שאליו נרצה להמיר (למשל "png").
     */
    public byte[] convertFormat(byte[] fileData, String sourceMimeType, String targetFormat) throws Exception 
    {
        // חילוץ הסיומת מתוך ה-MIME Type (למשל מ-"image/webp" נוציא "webp")
        String sourceExt = "bin";
        if (sourceMimeType != null && sourceMimeType.contains("/")) {
            sourceExt = sourceMimeType.substring(sourceMimeType.indexOf("/") + 1);
        }

        System.out.println("[CloudConvert] שלב 1: יצירת Job להמרה מ-" + sourceExt + " ל-" + targetFormat + "...");
        JSONObject jobResponse = createJob(targetFormat);
        String jobId = jobResponse.getJSONObject("data").getString("id");
        JSONObject uploadTask = jobResponse.getJSONObject("data").getJSONArray("tasks").getJSONObject(0);

        System.out.println("[CloudConvert] שלב 2: העלאת הקובץ לשרת...");
        // מעבירים גם את ה-MIME Type וגם את הסיומת
        uploadFile(uploadTask, fileData, sourceMimeType, sourceExt);

        System.out.println("[CloudConvert] שלב 3: המתנה לסיום ההמרה...");
        String downloadUrl = waitForResult(jobId);

        System.out.println("[CloudConvert] שלב 4: הורדת הקובץ המומר...");
        return downloadFileAsBytes(downloadUrl);
    }
    /**
     * יוצר משימה חדשה (Job) ב-CloudConvert, ומגדיר דינאמית את פורמט היעד להמרה.
     */
    private JSONObject createJob(String targetFormat) throws IOException 
    {
        // הזרקת פורמט היעד (targetFormat) לתוך ה-JSON של הבקשה
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

        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://api.cloudconvert.com/v2/jobs")
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful()) 
                throw new IOException("Failed to create job: " + response.body().string());
            
            return new JSONObject(response.body().string());
        }
    }

    /**
     * מעלה את מערך הבייטים של הקובץ לכתובת ההעלאה שהתקבלה מהשרת.
     */
    private void uploadFile(JSONObject uploadTask, byte[] fileData, String mimeType, String ext) throws IOException 
    {
        JSONObject form = uploadTask.getJSONObject("result").getJSONObject("form");
        String uploadUrl = form.getString("url");
        JSONObject parameters = form.getJSONObject("parameters");

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        
        for (String key : parameters.keySet())
            builder.addFormDataPart(key, parameters.get(key).toString());
        
        // התיקון החשוב: שימוש בסיומת האמיתית וב-MIME Type האמיתי של הקובץ!
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
     * ממתין (Polling) עד שהמשימה מסתיימת בשרת, ומחזיר את הקישור להורדה.
     */
    private String waitForResult(String jobId) throws Exception 
    {
        while (true) 
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
                

                JSONObject json = new JSONObject(response.body().string());
                String status = json.getJSONObject("data").getString("status");

                if (status.equals("finished")) 
                {
                    JSONArray tasks = json.getJSONObject("data").getJSONArray("tasks");
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
                    throw new Exception("ההמרה נכשלה בשרת CloudConvert.");
            
                Thread.sleep(3000); // המתנה של 3 שניות בין בדיקות
            }
        }
    }

    /**
     * מוריד את הקובץ המומר מהשרת ומחזיר אותו כמערך בייטים (במקום לשמור לדיסק).
     */
    private byte[] downloadFileAsBytes(String downloadUrl) throws IOException 
    {
        Request request = new Request.Builder().url(downloadUrl).get().build();
        try (Response response = client.newCall(request).execute()) 
        {
            if (!response.isSuccessful() || response.body() == null) 
            {
                throw new IOException("הורדת הקובץ נכשלה.");
            }
            // קריאת כל המידע למערך בייטים ישירות מהזרם של התשובה
            return response.body().bytes();
        }
    }
}