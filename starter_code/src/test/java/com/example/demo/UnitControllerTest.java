package com.example.demo;

import com.example.demo.controllers.CartController;
import com.example.demo.controllers.ItemController;
import com.example.demo.controllers.OrderController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class UnitControllerTest {

        private UserController userController;

        private ItemController itemController;

        private CartController cartController;

        private OrderController orderController;

        private UserRepository userRepository = mock(UserRepository.class);

        private CartRepository cartRepository = mock(CartRepository.class);

        private ItemRepository itemRepository = mock(ItemRepository.class);

        private OrderRepository orderRepository = mock(OrderRepository.class);

        private BCryptPasswordEncoder bCryptPasswordEncoder = mock(BCryptPasswordEncoder.class);

        @Before
        public void setup() {
            userController = new UserController();
            itemController = new ItemController();
            cartController = new CartController();
            orderController = new OrderController();
            ReflectionTestUtils.setField(userController, "userRepository", userRepository);
            ReflectionTestUtils.setField(userController, "cartRepository", cartRepository);
            ReflectionTestUtils.setField(userController, "bCryptPasswordEncoder", bCryptPasswordEncoder);

            ReflectionTestUtils.setField(itemController, "itemRepository", itemRepository);


            ReflectionTestUtils.setField(cartController, "userRepository", userRepository);
            ReflectionTestUtils.setField(cartController, "cartRepository", cartRepository);
            ReflectionTestUtils.setField(cartController, "itemRepository", itemRepository);

            ReflectionTestUtils.setField(orderController, "userRepository", userRepository);
            ReflectionTestUtils.setField(orderController, "orderRepository", orderRepository);
        }

        @Test
        public void createUserHappyPath() {
            CreateUserRequest createUserRequest = new CreateUserRequest();
            createUserRequest.setUsername("test");
            createUserRequest.setPassword("testPassword");
            createUserRequest.setConfirmPassword("testPassword");

            final ResponseEntity<User> response = userController.createUser(createUserRequest);

            assertNotNull(response);
            assertEquals(200, response.getStatusCodeValue());

            User user = response.getBody();
            assertNotNull(user);
            assertEquals("test", user.getUsername());
            assertNotEquals(createUserRequest.getPassword(), user.getPassword());
            assertEquals(bCryptPasswordEncoder.encode(createUserRequest.getPassword()), user.getPassword());

        }

        @Test
        public void createUserFailTest() {
            CreateUserRequest createUserRequest = new CreateUserRequest();
            createUserRequest.setUsername("test");
            createUserRequest.setPassword("testPassword");
            createUserRequest.setConfirmPassword("testPasswor");
            final ResponseEntity<User> response = userController.createUser(createUserRequest);
            assertNotNull(response);
            assertEquals(400, response.getStatusCodeValue());
            User user = response.getBody();
            assertNull(user);
        }

        @Test
        public void findUserByNameWithNotAuthenticated(){
            ResponseEntity<User> createdUser = userController.createUser(getCreateUserRequest());
            assertNotNull(createdUser);
            ResponseEntity<User> response = userController.findByUserName("test");
            assertEquals(404, response.getStatusCodeValue());
        }


        @Test
        public void findUserByName(){
            ResponseEntity<User> createdUser = userController.createUser(getCreateUserRequest());
            assertNotNull(createdUser);
            User user = createdUser.getBody();
            when(userRepository.findByUsername(user.getUsername())).thenReturn(user);

            ResponseEntity<User> response = userController.findByUserName("test");
            assertEquals(200, response.getStatusCodeValue());

            User user1 = response.getBody();

            assertNotNull(user1);
            assertEquals("test", user1.getUsername());
            assertEquals(bCryptPasswordEncoder.encode(getCreateUserRequest().getPassword()), user1.getPassword());

        }

        @Test
        public void findUserById(){
            ResponseEntity<User> createdUser = userController.createUser(getCreateUserRequest());
            User user =createdUser.getBody();
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

            ResponseEntity<User> response = userController.findById(1L);
            assertEquals(200, response.getStatusCodeValue());
            assertNotNull(response);

            User user1 = response.getBody();
            assertEquals("test", user1.getUsername());
            assertEquals(bCryptPasswordEncoder.encode(getCreateUserRequest().getPassword()), user1.getPassword());



        }

        @Test
        public void getAllItems(){
            itemRepository.save(getItem1());
            itemRepository.save(getItem2());
            when(itemRepository.findAll()).thenReturn(Collections.singletonList(getItem1()));

            ResponseEntity<List<Item>> allItems = itemController.getItems();

            List<Item> items = allItems.getBody();

            assertEquals(200, allItems.getStatusCodeValue());
            assertEquals(Long.valueOf(1L), items.get(0).getId());

        }

        @Test
        public void getItemById() {
            itemRepository.save(getItem1());
            itemRepository.save(getItem2());
            when(itemRepository.findById(1L)).thenReturn(java.util.Optional.of(getItem1()));

            ResponseEntity<Item> item = itemController.getItemById(1L);
            assertEquals(200, item.getStatusCodeValue());
            assertNotNull(item);

            Item item1 = item.getBody();
            assertNotNull(item1);
            assertEquals(Long.valueOf(1), item1.getId());
            assertEquals("iPad Air", item1.getName());


        }

        @Test
        public void getItemsByName() {
            itemRepository.save(getItem1());
            when(itemRepository.findByName("iPad Air")).thenReturn(Collections.singletonList(getItem1()));

            ResponseEntity<List<Item>> item = itemController.getItemsByName("iPad Air");
            assertEquals(200, item.getStatusCodeValue());
            assertNotNull(item);

            List<Item>  items = item.getBody();
            assertNotNull(items);
            assertEquals(Long.valueOf(1), items.get(0).getId());
            assertEquals("iPad Air", items.get(0).getName());

        }

        @Test
        public void addItemsToCartAndRemove(){

            userController.createUser(getCreateUserRequest());
            itemRepository.save(getItem1());
            when(itemRepository.findById(any())).thenReturn(java.util.Optional.of(getItem1()));
            when(userRepository.findByUsername(any())).thenReturn(getUser1());


            ResponseEntity<Cart> response = cartController.addTocart(getCartRequest());
            assertNotNull(response);

            Cart cart = response.getBody();
            assertEquals(Long.valueOf(1), cart.getId());
            assertEquals(getItem1(), cart.getItems().get(0));

            ResponseEntity<Cart> cartAfterRemove = cartController.removeFromcart(getCartRequest());
            assertNotNull(cartAfterRemove);
            assertEquals(200, cartAfterRemove.getStatusCodeValue());


        }

        @Test
        public void submitOrder(){
            userController.createUser(getCreateUserRequest());
            itemRepository.save(getItem1());
            when(itemRepository.findById(any())).thenReturn(java.util.Optional.of(getItem1()));
            when(userRepository.findByUsername(any())).thenReturn(getUser1());

            cartController.addTocart(getCartRequest());

            ResponseEntity<UserOrder> submittedOrder = orderController.submit("test");
            assertNotNull(submittedOrder);
            assertEquals(200, submittedOrder.getStatusCodeValue());

            UserOrder userOrder = submittedOrder.getBody();
            assertEquals("test", userOrder.getUser().getUsername());
            assertEquals(getItem1(), userOrder.getItems().get(0));

        }

        @Test
        public void getOrdersByName(){
            userController.createUser(getCreateUserRequest());
            itemRepository.save(getItem1());
            when(itemRepository.findById(any())).thenReturn(java.util.Optional.of(getItem1()));
            when(userRepository.findByUsername(any())).thenReturn(getUser1());
            when(orderRepository.findByUser(any())).thenReturn(Collections.singletonList(getUserOrder()));

            cartController.addTocart(getCartRequest());
            orderController.submit("test");

            ResponseEntity<List<UserOrder>> ordersOfUser = orderController.getOrdersForUser("test");
            assertNotNull(ordersOfUser);

            List<UserOrder> usersOrders = ordersOfUser.getBody();
            assertEquals(1, usersOrders.size());
            assertNotNull(usersOrders);
        }










        public static CreateUserRequest getCreateUserRequest(){
            CreateUserRequest createUserRequest = new CreateUserRequest();
            createUserRequest.setUsername("test");
            createUserRequest.setPassword("testPassword");
            createUserRequest.setConfirmPassword("testPassword");
            return createUserRequest;
        }

        public static Item getItem1(){
            Item item = new Item();
            item.setId(1L);
            item.setName("iPad Air");
            item.setPrice(BigDecimal.valueOf(300.00));
            return item;
        }

        public static Item getItem2(){
                Item item = new Item();
                item.setId(2L);
                item.setName("iPad Air Max");
                item.setPrice(BigDecimal.valueOf(400.00));
                return item;
        }

        public static User getUser1(){
            User user = new User();
            user.setUsername("test");
            user.setPassword("testPassword");
            user.setId(Long.valueOf(1));
            user.setCart(getCart());
            user.getCart().setUser(user);
            return user;
        }

    public static User getUserWithFullCart(){
        User user = new User();
        user.setUsername("test");
        user.setPassword("testPassword");
        user.setId(Long.valueOf(1));
        user.setCart(getCartWithItem());
        return user;
    }

        public static ModifyCartRequest getCartRequest() {
            ModifyCartRequest cartRequest = new ModifyCartRequest();
            cartRequest.setItemId(Long.valueOf(1));
            cartRequest.setQuantity(1);
            cartRequest.setUsername("test");
            return cartRequest;
        }

        public static Cart getCart() {
            Cart cart = new Cart();
            cart.setId(1L);
            return cart;
        }

        public static Cart getCartWithItem(){
            Cart cart = new Cart();
            cart.setId(1L);
            cart.addItem(getItem1());
            return cart;
        }

        public static UserOrder getUserOrder(){
            UserOrder userOrder = new UserOrder();
            userOrder.setId(1L);
            userOrder.createFromCart(getUserWithFullCart().getCart());
            return userOrder;
        }







}
