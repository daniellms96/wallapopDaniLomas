package com.example.wallapop.controller;

import com.example.wallapop.entities.Anuncio;
import com.example.wallapop.repositories.AnuncioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {
    @Autowired
    private AnuncioRepository anuncioRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<Anuncio> anuncios = anuncioRepository.findAllByOrderByFechaCreacionDesc();
        model.addAttribute("anuncios", anuncios);
        model.addAttribute("totalAnuncios", anuncios.size());
        return "index";
    }
}

