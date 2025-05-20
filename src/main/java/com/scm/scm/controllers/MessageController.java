package com.scm.scm.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.scm.scm.entities.Contact;
import com.scm.scm.entities.Message;
import com.scm.scm.entities.User;
import com.scm.scm.forms.MailForm;
import com.scm.scm.helpers.AppConstants;
import com.scm.scm.helpers.Helper;
import com.scm.scm.services.ContactService;
import com.scm.scm.services.MessageService;
import com.scm.scm.services.UserService;

// @RequestMapping("/user/message")
// public String viewMessageContacts(
// @RequestParam(value = "page", defaultValue = "0") int page,
// @RequestParam(value = "size", defaultValue = "" + AppConstants.PAGE_SIZE) int
// size,
// @RequestParam(value = "sortBy", defaultValue = "name") String sortBy,
// @RequestParam(value = "direction", defaultValue = "asc") String direction,
// Model model,
// Authentication authentication) {

// // 1. get current user
// String email = Helper.getEmailOfLoggedInUser(authentication);
// User user = userService.getUserByEmail(email);
// if (user == null) {
// model.addAttribute("error", "User not found");
// return "errorPage";
// }

// // 2. build Sort and Pageable
// Sort sort = Sort.by(sortBy);
// sort = "desc".equalsIgnoreCase(direction) ? sort.descending() :
// sort.ascending();
// PageRequest pageReq = PageRequest.of(page, size, sort);

// // 3. fetch paged contacts
// Page<Contact> pageContact = contactService.getByUser(user, pageReq);

// // 4. add to model
// model.addAttribute("pageContact", pageContact);
// model.addAttribute("pageSize", size);
// model.addAttribute("currentPage", page);
// model.addAttribute("totalPages", pageContact.getTotalPages());
// model.addAttribute("sortBy", sortBy);
// model.addAttribute("direction", direction);
// model.addAttribute("navbarPadding", "my-custom-pl-64");

// return "user/message";
// }

@Controller
@RequestMapping("/user/message")
public class MessageController {
    @Autowired
    private MessageService messageService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender mailSender;

    @RequestMapping
    public String getChat(Model model, Authentication authentication) {

        List<User> users = userService.getAllUsers();
        String email = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(email);
        String senderId = user.getUserId(); // assuming your User class has ID
        List<User> filteredUsers = users.stream()
                .filter(u -> !u.getUserId().equals(user.getUserId())) // remove self
                .filter(u -> !u.getRole().equalsIgnoreCase("ROLE_ADMIN")) // remove admins
                .toList();
        if (filteredUsers.isEmpty()) {
            return "user/chat";
        }
        String receiverId = filteredUsers.get(0).getUserId();
        List<Message> messages = messageService.getConversation(senderId, receiverId);
        model.addAttribute("users", filteredUsers);
        model.addAttribute("messages", messages);
        model.addAttribute("receiverId", receiverId);

        return "user/chat";
    }

    @GetMapping("/{receiverId}")
    public String getReceiverChat(@PathVariable("receiverId") String receiverId, Model model,
            Authentication authentication) {

        List<User> users = userService.getAllUsers();
        String email = Helper.getEmailOfLoggedInUser(authentication);
        User user = userService.getUserByEmail(email);
        String senderId = user.getUserId(); // assuming your User class has ID
        List<User> filteredUsers = users.stream()
                .filter(u -> !u.getUserId().equals(user.getUserId())) // remove self
                .filter(u -> !u.getRole().equalsIgnoreCase("ROLE_ADMIN")) // remove admins
                .toList();
        List<Message> messages = messageService.getConversation(senderId, receiverId);
        model.addAttribute("users", filteredUsers);
        model.addAttribute("messages", messages);
        model.addAttribute("receiverId", receiverId);
        return "user/chat";
    }

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendMessage(@RequestBody Map<String, String> payload,
            Authentication authentication) {
        String email = Helper.getEmailOfLoggedInUser(authentication);
        User sender = userService.getUserByEmail(email);

        String receiverId = payload.get("receiverId");
        String text = payload.get("messageText");

        messageService.sendMessage(sender.getUserId(), receiverId, text);

        Map<String, String> response = new HashMap<>();
        response.put("messageText", text);
        return ResponseEntity.ok(response);
    }

    // ——— Show compose form ———
    @GetMapping("/mail/{contactId}")
    public String composeMailForm(@PathVariable String contactId,
            Model model, Authentication auth) {
        String email = Helper.getEmailOfLoggedInUser(auth);
        User user = userService.getUserByEmail(email);
        Contact contact = contactService.getById(contactId);
        if (!contact.getUser().getUserId().equals(user.getUserId()))
            return "error/403";

        MailForm mailForm = new MailForm();
        mailForm.setTo(contact.getEmail());

        model.addAttribute("contact", contact);
        model.addAttribute("mailForm", mailForm);
        model.addAttribute("loggedInUser", user);
        // model.addAttribute("navbarPadding", "my-custom-pl-64");

        return "user/mail_compose";
    }

    // ——— Handle send action ———
    @PostMapping("/mail/{contactId}")
    public String sendMail(@PathVariable String contactId,
            @ModelAttribute MailForm mailForm,
            Authentication auth,
            RedirectAttributes redirect) {

        Contact contact = contactService.getById(contactId);
        // optionally verify ownership as above…

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(mailForm.getTo());
        msg.setSubject(mailForm.getSubject());
        msg.setText(mailForm.getBody());
        mailSender.send(msg);

        redirect.addFlashAttribute("message",
                "Email sent to " + contact.getName());
        return "redirect:/user/contacts";
    }
}
