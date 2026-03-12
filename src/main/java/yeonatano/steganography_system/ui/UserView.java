package yeonatano.steganography_system.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import yeonatano.steganography_system.datamodels.User;
import yeonatano.steganography_system.services.UserService;



@Route("/users")
public class UserView extends VerticalLayout implements BeforeEnterObserver
{
   private UserService userService;
   private TextField txfUN, txfPW;
   private Grid<User> usersGrid;


public UserView(UserService userService)
   {
      this.userService = userService;

      txfUN = new TextField("Username");
      txfPW = new TextField("Password");
      usersGrid = new Grid<>(User.class);


      Button btnAddUser = new Button("+ Add User", e -> addUserToDB(txfUN.getValue(), txfPW.getValue()));

      HorizontalLayout fieldsPanel = new HorizontalLayout();
      fieldsPanel.setWidthFull();
      fieldsPanel.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
      fieldsPanel.add(txfUN, txfPW, btnAddUser);


      usersGrid.setItems(userService.getAllUsers());
      usersGrid.getStyle().setBorder("1px solid gray");

      add(fieldsPanel, usersGrid);


   }

   private void addUserToDB(String un, String pw)
   {
      User userToAdd = new User(un,pw);
      boolean res = userService.addUserToDB(userToAdd);

      if(res)
      {
         txfUN.clear();
         txfPW.clear();
         usersGrid.setItems(userService.getAllUsers());
      }


   }

   @Override
   public void beforeEnter(BeforeEnterEvent event)
   {

   }
   }   

