package com.example._3dinspire_serveur.repository;

import com.example._3dinspire_serveur.model.Avis;
import com.example._3dinspire_serveur.model.Tag;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TagRespository extends CrudRepository<Tag, String> {

}