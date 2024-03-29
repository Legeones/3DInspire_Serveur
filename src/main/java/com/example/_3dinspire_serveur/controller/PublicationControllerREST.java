package com.example._3dinspire_serveur.controller;

import com.example._3dinspire_serveur.model.Avis;
import com.example._3dinspire_serveur.model.DTO.AvisDTO;
import com.example._3dinspire_serveur.model.Publication;
import com.example._3dinspire_serveur.model.Tag;
import com.example._3dinspire_serveur.model.Utilisateur;
import com.example._3dinspire_serveur.repository.AvisRepository;
import com.example._3dinspire_serveur.repository.PublicationRepository;
import com.example._3dinspire_serveur.repository.TagRespository;
import com.example._3dinspire_serveur.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controlleur des Publication en Rest
 */
@RestController
@RequestMapping("/publication")
public class PublicationControllerREST {
    private final PublicationRepository publicationRepository;

    private final UtilisateurRepository utilisateurRepository;
    private final AvisRepository avisRepository;
    private final TagRespository tagRespository;

    @Value("${file.upload-dir-model}")
    private String uploadDirModel;
    @Value("${file.upload-dir-image}")
    private String uploadDirImage;

    public PublicationControllerREST(PublicationRepository publicationRepository, UtilisateurRepository utilisateurRepository, AvisRepository avisRepository, TagRespository tagRespository) {
        this.publicationRepository = publicationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.avisRepository = avisRepository;
        this.tagRespository = tagRespository;
    }

    /**
     * Renvoie la publication en fonction de l'ID
     * @param id ID de la Publication
     * @return Publication
     */
    @GetMapping("/get/{id}")
    public Publication getPublication(
            @PathVariable("id") Long id
    ){
        if (publicationRepository.findById(id).isPresent())
        return publicationRepository.findById(id).get();
        else return null;
    }

    /**
     * Renvoie les Publication d'un Utilisateur
     * @param id ID de l'Utilisateur
     * @return Iterable<Publication>
     */
    @GetMapping("/get/uti/{id}")
    public Iterable<Publication> getPublicationByUtilisateurId(
            @PathVariable("id") Long id
    ){
        if (utilisateurRepository.findById(id).isPresent())
            return publicationRepository.getPublicationByProprietaireId(utilisateurRepository.findById(id).get());
        else return null;
    }

    /**
     * Retourne toutes les Publication
     * @return Iterable Publication
     */
    @GetMapping("/getAll")
    public ResponseEntity<Iterable<Publication>> getAllPublication() {
        Iterable<Publication> publications = publicationRepository.findAll();
        ResponseEntity<Iterable<Publication>> responseEntity = ResponseEntity.ok().body(publications);
        responseEntity.getHeaders().forEach((headerName, headerValues) ->
                System.out.println(headerName + ": " + headerValues));

        return responseEntity;
    }

    /**
     * Retourne toutes les Publication dans un ordre décroissant en fonction du temps
     * @return Iterable Publication
     */
    @GetMapping("/getAllByTime")
    public ResponseEntity<Iterable<Publication>> getAllPublicationByTime() {
        Iterable<Publication> publications = publicationRepository.getPublicationByTime();
        ResponseEntity<Iterable<Publication>> responseEntity = ResponseEntity.ok().body(publications);
        responseEntity.getHeaders().forEach((headerName, headerValues) ->
                System.out.println(headerName + ": " + headerValues));

        return responseEntity;
    }

    /**
     * Retourne les Publication en fonction du filtre entrer par l'Utilisateur
     * Cherche dans le Titre/Pseudo(Créateur)/Tags
     * @param filtre Chaine de caractère pour le filtre
     * @return Iterable Publication
     */
    @GetMapping("/getByFiltre")
    public ResponseEntity<Iterable<Publication>> getByFiltre(@RequestParam("filtre")String filtre){
        System.out.println(filtre);
        Iterable<Publication> publications = publicationRepository.getFiltre(filtre);
        ResponseEntity<Iterable<Publication>> responseEntity = ResponseEntity.ok().body(publications);
        responseEntity.getHeaders().forEach((headerName, headerValues) ->
                System.out.println(headerName + ": " + headerValues));

        return responseEntity;
    }

    /**
     * Retourne les Publication achetées par l'Utilisateur
     * @param id ID de l'Utilisateur
     * @return Iterable Publication
     */
    @GetMapping("/getPubAchete/{id}")
    public ResponseEntity<Set<Publication>> getPubAcheteById(@PathVariable("id") Long id){
        Optional<Utilisateur> utilisateurOp = utilisateurRepository.findById(id);
        if (utilisateurOp.isPresent()) {
            Utilisateur utilisateur = utilisateurOp.get();
            Set<Publication> publications = utilisateur.getPublicationsAchats();
            return new ResponseEntity<>(publications, HttpStatus.OK);

        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Retourne les Publication avec un filtre de recherche pour l'Utilisateur (créateur)
     * @param id ID de l'Utilisateur
     * @param filtre Chaine de caractère pour la recherche
     * @return Iterable Publication
     */
    @GetMapping("/get/uti/{id}/getFiltreByUser")
    public ResponseEntity<Iterable<Publication>> getFiltreByUser(@PathVariable("id") Long id,@RequestParam("filtre")String filtre){
        if (utilisateurRepository.findById(id).isPresent()) {
            Iterable<Publication> publications = publicationRepository.getFiltreByUser(filtre,id);
            ResponseEntity<Iterable<Publication>> responseEntity = ResponseEntity.ok().body(publications);
            responseEntity.getHeaders().forEach((headerName, headerValues) ->
                    System.out.println(headerName + ": " + headerValues));
            return responseEntity;
        }
        else return null;

    }

    /**
     * Création de la Publication
     * @param titre Titre de la publication
     * @param description Description de la publication
     * @param gratuit Boolean de gratuité
     * @param publique Boolean de visibilité
     * @param prix Flottant du prix (si gratuit = faux)
     * @param image Chaine de caractères du nom de l'image
     * @param file Chaine de caractères du nom du fichier
     * @param tags Chaine de caractères des tags de la publication (#..)
     * @param email Email de l'Utilisateur
     * @return Publication
     */
    @PostMapping("/save")
    public Publication savePublication(
            @RequestParam("titre") String titre,
            @RequestParam("description") String description,
            @RequestParam("gratuit") boolean gratuit,
            @RequestParam("publique") boolean publique,
            @RequestParam("prix") float prix,
            @RequestParam("image") MultipartFile image,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tags") List<String> tags,
            @RequestParam("email") String email
            ) {

        // Créer un objet Publication à partir des paramètres
        Publication nouvellePublication = new Publication();
        nouvellePublication.setTitre(titre);
        nouvellePublication.setDescription(description);
        nouvellePublication.setGratuit(gratuit);
        nouvellePublication.setPublique(publique);
        nouvellePublication.setPrix(prix);
        nouvellePublication.setNb_telechargement(0);
        nouvellePublication.setFichier("_");
        nouvellePublication.setImage("_");
        nouvellePublication.setDateLocal(LocalDateTime.now());
        Publication publication = publicationRepository.save(nouvellePublication);
        String nouvemail = email.replaceAll("\"", "");

        Optional<Utilisateur> utilisateurOptional = Optional.ofNullable(utilisateurRepository.findByEmail(nouvemail));
        Utilisateur utilisateur = utilisateurOptional.orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé pour l'e-mail " + email));

        publication.setImage(isEmpty(image, publication.getId(), utilisateur.getId(), titre, "i"));
        publication.setFichier(isEmpty(file, publication.getId(), utilisateur.getId(), titre, "f"));

        for (String tagNom : tags) {
            String tagNomSansGuillemets = tagNom.replaceAll("\"", "");  // Enlever les guillemets
            Optional<Tag> tagOptional = tagRespository.findByNom(tagNomSansGuillemets);

            Tag tag = tagOptional.orElseGet(() -> {
                Tag nouveauTag = new Tag();
                nouveauTag.setNom(tagNomSansGuillemets);
                return tagRespository.save(nouveauTag);
            });

            // Associer la publication au tag et vice versa
            publication.getTags().add(tag);
            tag.getPublications().add(publication);
        }

        utilisateurOptional.ifPresent(publication::setProprietaire);
        return publicationRepository.save(publication);
    }

    /**
     * Retourne le Fichier avec son path
     * @param object Fichier
     * @param publication_id ID de la Publication
     * @param proprietaire_id ID du Proprietaire de la Publication
     * @param titre Titre de la publication
     * @param type m -> fichier 3D, i -> image
     * @return File
     */
    public String isEmpty(MultipartFile object, long publication_id, long proprietaire_id, String titre, String type){
        if (object.isEmpty()) {
            System.out.println("Fichier est vide");
        } else {
            try {
                String lien = System.currentTimeMillis() + "_" + publication_id + "_" + proprietaire_id + "_" + titre + "_" + object.getOriginalFilename();
                File dest;
                if (Objects.equals(type, "i")){
                    dest = new File(uploadDirImage + File.separator + lien);
                } else {
                    dest = new File(uploadDirModel + File.separator + lien);
                }
                object.transferTo(dest);
                return lien;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "_";
    }

    /**
     * Supprimer la publication
     * @param id ID de la publication
     */
    @DeleteMapping("/delete/{id}")
    public void deletePublication(
            @PathVariable("id") Long id
    ){
        publicationRepository.deleteById(id);
        avisRepository.deleteAvisByPublicationId(id);
    }

    /**
     * Retourne les Avis de la Publication actuel
     * @param id ID de la Publication
     * @return Iterable Avis
     */
    @GetMapping("/avis/get/pub/{id}")
    public Iterable<AvisDTO> getAllAvisByPublication(
            @PathVariable("id") Long id
    ){
        Iterable<Avis> avisList = avisRepository.getAvisByPublicationId(publicationRepository.findById(id).get());
        List<AvisDTO> avisDTOList = new ArrayList<>();

        for (Avis avis : avisList) {
            System.out.println(avis.getUtilisateur().getId());
            avisDTOList.add(new AvisDTO(avis.getId(),
                    avis.getCommentaire(),
                    avis.getEtoile(),
                    avis.getPublication().getId(),
                    avis.getUtilisateur().getId()));
        }
        return avisDTOList;
    }

    /**
     * Retourne les Avis de l'Utilisateur actuel
     * @param id ID de l'Utilisateur
     * @return Iterable Avis
     */
    @GetMapping("/avis/get/uti/{id}")
    public Iterable<AvisDTO> getAllAvisByUtilisateur(
            @PathVariable("id") Long id
    ){
        Iterable<Avis> avisList = avisRepository.getAvisByUtilisateurId(utilisateurRepository.findById(id).get());
        List<AvisDTO> avisDTOList = new ArrayList<>();

        for (Avis avis : avisList) {
            avisDTOList.add(new AvisDTO(avis.getId(),
                    avis.getCommentaire(),
                    avis.getEtoile(),
                    avis.getPublication().getId(),
                    avis.getUtilisateur().getId()));
        }
        return avisDTOList;
    }

//    @PostMapping("/telechargement/{id}")
//    public void telechargementByIDPub(@PathVariable("id") Long id) {
//        Publication publication = publicationRepository.getPublicationById(id);
//
//        if(publication!=null){
//            int nbTele = publication.getNb_telechargement();
//            publication.setNb_telechargement(nbTele+1);
//            publicationRepository.save(publication);
//        }
//        else{
//            System.out.println("la publication téléchargé n'existe pas");
//        }
//    }

    /**
     * Retourne tous les avis
     * @return Iterable Avis
     */
    @GetMapping("/avis/get")
    public Iterable<AvisDTO> getAllAvis() {
        Iterable<Avis> avisList = avisRepository.findAll();
        List<AvisDTO> avisDTOList = new ArrayList<>();

        for (Avis avis : avisList) {
            avisDTOList.add(new AvisDTO(avis.getId(),
                    avis.getCommentaire(),
                    avis.getEtoile(),
                    avis.getPublication().getId(),
                    avis.getUtilisateur().getId()));
        }

        return avisDTOList;
    }

    /**
     * Ajouter un Avis
     * @param commentaire Commentaire de la Publication
     * @param etoile Note octroyé
     * @param publication_id ID de la Publication concerné
     * @param utilisateur_id ID de l'Utilisateur faisant l'Avis
     * @return Avis
     */
    @PostMapping("/avis/save")
    public Avis saveAvis(
            @RequestParam("commentaire") String commentaire,
            @RequestParam("etoile") int etoile,
            @RequestParam("publication") long publication_id,
            @RequestParam("utilisateur") long utilisateur_id
    ) {
        Avis avis = new Avis();
        avis.setCommentaire(commentaire);
        avis.setEtoile(etoile);

        Optional<Publication> publicationOptional = publicationRepository.findById(publication_id);
        publicationOptional.ifPresentOrElse(
                avis::setPublication, // Consumer for if the Optional is present
                new Runnable() { // Runnable for if the Optional is empty
                    @Override
                    public void run() {
                        // Handle the case where publicationOptional is not present
                        System.err.println("Publication not found for ID: " + publication_id);
                        new Throwable().printStackTrace(); // Print the stack trace
                    }
                }
        );
        
        Optional<Utilisateur> utilisateurOptional = utilisateurRepository.findById(utilisateur_id);
        utilisateurOptional.ifPresent(avis::setUtilisateur);
        utilisateurOptional.ifPresentOrElse(
                avis::setUtilisateur,
                new Runnable() {
                    @Override
                    public void run() {
                        // Handle the case where publicationOptional is not present
                        System.err.println("Publication not found for ID: " + utilisateur_id);
                        new Throwable().printStackTrace(); // Print the stack trace
                    }
                }
        );

        return avisRepository.save(avis);
    }


}