package com.example.wallapop.services;

import com.example.wallapop.entities.Usuario;
import com.example.wallapop.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario findByUsername(String username) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        return usuarioOpt.orElse(null);
    }
}



