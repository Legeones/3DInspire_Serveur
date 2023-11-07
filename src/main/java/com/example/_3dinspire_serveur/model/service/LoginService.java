package com.example._3dinspire_serveur.model.service;

import com.example._3dinspire_serveur.model.Utilisateur;
import com.example._3dinspire_serveur.model.respository.LoginRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    private final LoginRespository loginRepository;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    public LoginService(LoginRespository loginRepository) {
        this.loginRepository = loginRepository;
    }

    public int registerNewUser(String email, String password, String pseudo) {
        return loginRepository.registerNewUser(email, pseudo, password);
    }
    public Utilisateur registerNewUser2(String email, String password, String pseudo) {
        Utilisateur uti = new Utilisateur(email,pseudo,password);
        return loginRepository.save(uti);
    }
    public void inscription(Utilisateur utilisateur) {
        String mdp_hash = this.bCryptPasswordEncoder.encode(utilisateur.getPassword());
        utilisateur.setPassword(mdp_hash);
        this.loginRepository.save(utilisateur);
    }
}
