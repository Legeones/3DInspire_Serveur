package com.example._3dinspire_serveur.controller;

import com.example._3dinspire_serveur.model.Notification;
import com.example._3dinspire_serveur.model.Profil;
import com.example._3dinspire_serveur.model.Utilisateur;
import com.example._3dinspire_serveur.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

import java.util.Optional;

@RestController
public class UtilisateurControllerREST {

    @Value("${file.upload-dir-image-profil}")
    private String uploadDirImageProfil;
    private final UtilisateurRepository utilisateurRepository;
    private final ProfilRepository profilRepository;
    private final PublicationRepository publicationRepository;
    private final NotificationRepository notificationRepository;
    private final AvisRepository avisRepository;

    public UtilisateurControllerREST(UtilisateurRepository utilisateurRepository, ProfilRepository profilRepository, PublicationRepository publicationRepository, NotificationRepository notificationRepository, AvisRepository avisRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilRepository = profilRepository;
        this.publicationRepository = publicationRepository;
        this.notificationRepository = notificationRepository;
        this.avisRepository = avisRepository;
    }


    @PostMapping("/saveUtilisateur")
    public Utilisateur ajoutUtilisateur(@ModelAttribute@Valid Utilisateur utilisateur, @ModelAttribute@Valid Profil profil){
        profilRepository.save(profil);
        utilisateur.setProfil(profil);
        return utilisateurRepository.save(utilisateur);
    }
 // fonction pour s abonner
    @GetMapping("/abonnenement/{user}/{id}")
    public void abonnenement(@PathVariable Long id, @PathVariable Long user){
        Optional<Utilisateur> utilisateur_abonne = utilisateurRepository.findById(id);
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(user);
        if (utilisateur_abonne.isPresent() && utilisateur.isPresent()){
            if(!id.equals(user)) {
                Notification notification = new Notification(null, LocalDate.now(),null);
                utilisateur.get().ajouterAbonnement(utilisateur_abonne.get());
                utilisateur_abonne.get().ajouterAbonne(utilisateur.get());
                System.out.println(notification.getId());
                utilisateur_abonne.get().ajouterNotification(notification);
                notification.setUtilisateur(utilisateur_abonne.get());

                notificationRepository.save(notification);
                utilisateurRepository.save(utilisateur.get());
                utilisateurRepository.save(utilisateur_abonne.get());
            }
        }
    }

    // fonction pour se desabonner
    @GetMapping("/desabonnemnt/{user}/{id}")
    public void desabonnenement(@PathVariable Long id, @PathVariable Long user){
        Optional<Utilisateur> utilisateur_abonne = utilisateurRepository.findById(id);
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(user);
        if (utilisateur_abonne.isPresent() && utilisateur.isPresent()){
            utilisateur.get().deleteAbonnement(utilisateur_abonne.get());
            utilisateur_abonne.get().deleteAbonne(utilisateur.get());
            utilisateurRepository.save(utilisateur_abonne.get());
            utilisateurRepository.save(utilisateur.get());
        }
    }

    // fonction pour avoir un json de mon utilisateur
    @GetMapping("/userInformation/{user}")
    public Map<String, String> userInformation(@PathVariable Long user) throws UnsupportedEncodingException {
        Map<String, String> information = new HashMap<>();
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(user);

        System.out.println("salut");
        if (utilisateur.isPresent()) {
            Utilisateur userObj = utilisateur.get();

            information.put("idUtilisateur" , String.valueOf(userObj.getId()));
            information.put("countAbonnement", String.valueOf(userObj.countAbonnement()));
            information.put("countAbonne", String.valueOf(userObj.countAbonne()));
            information.put("pseudo", String.valueOf(userObj.getPseudo()));

            Profil profil = userObj.getProfil();
//            if (profil != null) {
            if (profil.getDescription() != null) {
                String encodedDescription = URLEncoder.encode(profil.getDescription(), StandardCharsets.UTF_8);
                information.put("description", encodedDescription);
            }else {
                information.put("description", null);
            }
            if (profil.getPhoto() != null) {
                information.put("photo", profil.getPhoto());
            } else {
                information.put("photo", null);
            }
        }

        return information;
    }

    // fonction pour avoir une liste de tout les abonnements d'un utilisateur
    @GetMapping("/abonnementUser/{userId}")
    public Set<Map> AbonnementUser(@PathVariable Long userId) throws UnsupportedEncodingException {
        Set<Map> abonnementinfo = new HashSet<>();
        Optional<Utilisateur> utilisateurOptional = utilisateurRepository.findById(userId);
        if (utilisateurOptional.isPresent()) {
            Utilisateur utilisateur = utilisateurOptional.get();
            for (Utilisateur utilisateur1 : utilisateur.getAbonnements()) {
                abonnementinfo.add(userInformation(utilisateur1.getId()));

            }
            return abonnementinfo;
        } else {
            // L'utilisateur avec l'ID spécifié n'a pas été trouvé
            return Collections.emptySet();
        }
    }

    // fonction pour avoir une liste de tout les abonnes d'un utilisateur

    @GetMapping("/abonneUser/{userId}")
    public Set<Map<String,String>> AbonneUser(@PathVariable Long userId) throws UnsupportedEncodingException {
        Set<Map<String,String>> abonneinfo = new HashSet<>();
        Optional<Utilisateur> utilisateurOptional = utilisateurRepository.findById(userId);
        if (utilisateurOptional.isPresent()) {

            Utilisateur utilisateur = utilisateurOptional.get();
            for (Utilisateur utilisateur1 : utilisateur.getAbonnes()) {
                abonneinfo.add(userInformation(utilisateur1.getId()));

            }
            System.out.println(abonneinfo);
            return abonneinfo;
        } else {
            // L'utilisateur avec l'ID spécifié n'a pas été trouvé
            return Collections.emptySet();
        }
    }


    // fonction pour voir si un abonné est présent dans mes abonnements
    @GetMapping("/presenceAbonne/{userId}/{abonneid}")
    public boolean presenceAbonne(@PathVariable Long userId, @PathVariable Long abonneid) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(userId);
        Optional<Utilisateur> abonneUser = utilisateurRepository.findById(abonneid);

        if (utilisateur.isPresent() && abonneUser.isPresent()) {
            return utilisateur.get().verifAbonnement(abonneUser.get());
        } else {
            return false;
        }
    }

    @GetMapping("/presenceUserNotifie/{userId}/{abonnement}")
    public boolean presenceUserNotifie(@PathVariable Long userId, @PathVariable Long abonnementid) {
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(userId);
        Optional<Utilisateur> abonnementUser = utilisateurRepository.findById(abonnementid);

        if (utilisateur.isPresent() && abonnementUser.isPresent()) {
            return utilisateur.get().verifUserNotifies(abonnementUser.get());
        } else {
            return false;
        }
    }

    @GetMapping("/getUtilisateurIdByEmail")
    public Long getUtilisateurIdByEmail(@RequestParam String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email);
        System.out.println(utilisateur.getId());
        return utilisateur.getId();
    }


    @GetMapping("/getUtilisateur/{id}")
    public Utilisateur getUtilisateurById(@PathVariable("id") Long id) {
        if (utilisateurRepository.findById(id).isPresent()) {
            Utilisateur user = new Utilisateur();
            user.setId(utilisateurRepository.findById(id).get().getId());
            user.setPseudo(utilisateurRepository.findById(id).get().getPseudo());
            return user;
        }
        return new Utilisateur();
    }

    @Transactional
    @DeleteMapping("/utilisateur/delete/{id}")
    public void deleteUtilisateur( @PathVariable("id") Long id) {
        try {
            avisRepository.deleteAvisByUtilisateurId(id);
            publicationRepository.deletePublicationByProprietaire(id);
            profilRepository.deleteProfilByUtilisateurId(id);
            utilisateurRepository.deleteById(id);
            System.out.println("Utilisateur supprimé avec succès.");
        } catch (Exception e) {
            System.out.println("Erreur lors de la suppression de l'utilisateur.");
        }
    }





    // fonction pour enregistrer une photo dans la bdd

//    @PostMapping("/upload/{user}")
//    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file, @PathVariable Long user) throws IOException {
//        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(user);
//        if (utilisateur.isPresent()){
//            if (utilisateur.get().getProfil() != null) {
//                Optional<Profil> profil = profilRepository.findById(utilisateur.get().getProfil().getId());
//                if (profil.isPresent()){
//                    System.out.println(profil.get().getId());
//                    profil.get().setPhoto(file.getBytes());
//                    utilisateur.get().setProfil(profil.get());
//                    profilRepository.save(profil.get());
//                }
//            }else {
//                Profil profil = new Profil(null, file.getBytes(),null);
//                utilisateur.get().setProfil(profil);
//                profilRepository.save(profil);
//            }
//
//        }
//        return ResponseEntity.ok("Image téléchargée avec succès !");
//    }


    // fonction pour enregistrer les string du compte, pseudo, description

    @PostMapping("/updateStringProfil/{user}")
    public ResponseEntity<String> handleFileUpload(
            @RequestBody Map<String, String> requestBody, @PathVariable Long user) {
        System.out.println("salutation77");
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(user);
        if (utilisateur.isPresent()){
            utilisateur.get().setPseudo(requestBody.get("pseudo").replaceAll("^\"|\"$", ""));
            utilisateurRepository.save(utilisateur.get());
                Optional<Profil> profil = profilRepository.findById(utilisateur.get().getProfil().getId());
                if (profil.isPresent()){
                    System.out.println("666666666"+requestBody.get("description"));
                    if (requestBody.get("description").length() > 0) {
                        System.out.println(requestBody.get("description").length());
                        profil.get().setDescription(requestBody.get("description").replaceAll("^\"|\"$", ""));
                    }else {
                        profil.get().setDescription(null);
                    }
                    profilRepository.save(profil.get());
                }
        }
        return ResponseEntity.ok("OK");
    }

    private ResponseEntity<Resource> getResourceResponseEntity(@PathVariable String nomFichier, String uploadDir) throws MalformedURLException {
        Path filePath = Paths.get(uploadDir).resolve(nomFichier).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } else {
            // Gérer l'erreur si le fichier n'est pas trouvé
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/imageProfil/{nomFichier:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String nomFichier) throws MalformedURLException {
        return getResourceResponseEntity(nomFichier, uploadDirImageProfil);
    }

    @PostMapping("/uploadProfil/{user}")
    public ResponseEntity<String> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @PathVariable("user") long user) {
        System.out.println("test1212121221212");
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(user);
        try {
            if (utilisateur.isPresent()) {
//                if (utilisateur.get().getProfil() != null) {

                    if (utilisateur.get().getProfil().getPhoto() != null) {
                        Path filePath = Paths.get(uploadDirImageProfil).resolve(utilisateur.get().getProfil().getPhoto()).normalize();

                        // Vérifier si le fichier existe avant de le supprimer
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                            System.out.println("supprime");
                        } else {
                            System.out.println("erreur");
                        }
                    }
                    // Construire le chemin complet pour le nouveau fichier
                    Path targetLocation = Path.of(uploadDirImageProfil).resolve(file.getOriginalFilename());

                    utilisateur.get().getProfil().setPhoto(file.getOriginalFilename());
                    utilisateurRepository.save(utilisateur.get());
                    // Copier le fichier dans le répertoire de destination
                    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("Fichier enregistré avec succès à : " + targetLocation);

                    return ResponseEntity.ok("Fichier téléchargé avec succès!");
//                } else {
//                    // Construire le chemin complet pour le nouveau fichier
//                    Path targetLocation = Path.of(uploadDirImageProfil).resolve(file.getOriginalFilename());
//
//                    utilisateur.get().getProfil().setPhoto(file.getOriginalFilename());
//                    utilisateurRepository.save(utilisateur.get());
//                    // Copier le fichier dans le répertoire de destination
//                    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
//
//                    System.out.println("Fichier enregistré avec succès à : " + targetLocation);
//                    Profil profil = new Profil(null, file.getOriginalFilename(), null);
//                    utilisateur.get().setProfil(profil);
//                    profilRepository.save(profil);
//                    utilisateurRepository.save(utilisateur.get());
//                    return ResponseEntity.ok("Fichier téléchargé avec succès!");
//                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la suppression du fichier.");
        }
        return null;

    }
}

