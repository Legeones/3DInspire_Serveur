package com.example._3dinspire_serveur.controller;

import com.example._3dinspire_serveur.model.Utilisateur;
import com.example._3dinspire_serveur.model.service.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aide")
public class AideController {
    @Autowired
    private JavaMailSender javaMailSender;
    private Environment env;
    private UtilisateurService userService;

    public AideController(UtilisateurService userService) {
        this.userService = userService;
    }

    public void MailService(Environment env){
        this.env = env;

    }
    @PostMapping("/mailAide")
    private void sendPasswordResetEmail(@RequestParam String email, @RequestParam String aide) {
        Utilisateur user = userService.findUserByEmail(email);

        if (user == null) {
            System.out.println("l'utilisateur n'existe pas");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(email);
            message.setTo("3dinspire10@gmail.com");
            message.setSubject("Demande d'aide de "+email);
            message.setText(aide);

            System.out.println("About to send email...");

            javaMailSender.send(message);

            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            System.out.println("Failed to send email. Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}