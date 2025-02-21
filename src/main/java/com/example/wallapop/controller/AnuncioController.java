package com.example.wallapop.controller;

import com.example.wallapop.entities.Anuncio;
import com.example.wallapop.entities.Foto;
import com.example.wallapop.entities.Usuario;
import com.example.wallapop.repositories.AnuncioRepository;
import com.example.wallapop.repositories.FotoRepository;
import com.example.wallapop.repositories.UsuarioRepository;
import com.example.wallapop.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.wallapop.services.AnuncioService;


import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Controller
@RequestMapping("/anuncios")
public class AnuncioController {

    @Autowired
    private AnuncioRepository anuncioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private FotoRepository fotoRepository;

    private final AnuncioService anuncioService;
    private final UsuarioService usuarioService;

    @Autowired
    public AnuncioController(AnuncioService anuncioService, UsuarioService usuarioService) {
        this.anuncioService = anuncioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(@RequestParam(value = "id", required = false) Long id, Model model) {
        if (id != null) {
            Optional<Anuncio> anuncioExistente = anuncioRepository.findById(id);
            if (anuncioExistente.isPresent()) {
                model.addAttribute("anuncio", anuncioExistente.get());
            } else {
                model.addAttribute("anuncio", new Anuncio());
            }
        } else {
            model.addAttribute("anuncio", new Anuncio());
        }
        return "formulario-anuncio";
    }

    @PostMapping("/guardar")
    public String guardarAnuncio(@RequestParam(required = false) Long id,
                                 @RequestParam String titulo,
                                 @RequestParam double precio,
                                 @RequestParam(required = false) String descripcion,
                                 @RequestParam(value = "fotos", required = false) MultipartFile[] fotos,
                                 @RequestParam(value = "eliminarFotos", required = false) List<Long> eliminarFotos,
                                 Model model) {

        // Obtener el usuario autenticado.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        if (usuario == null) {
            return "redirect:/login";
        }

        Anuncio anuncio;

        if (id != null) {
            Optional<Anuncio> anuncioExistente = anuncioRepository.findById(id);
            if (anuncioExistente.isPresent()) {
                anuncio = anuncioExistente.get();

                // Verificar que el usuario autenticado es el dueño
                if (!anuncio.getUsuario().getId().equals(usuario.getId())) {
                    return "redirect:/?error=No tienes permiso para modificar este anuncio";
                }
            } else {
                anuncio = new Anuncio();
                anuncio.setUsuario(usuario);
            }
        } else {
            anuncio = new Anuncio();
            anuncio.setUsuario(usuario);
        }

        anuncio.setTitulo(titulo);
        anuncio.setPrecio(precio);
        anuncio.setDescripcion(descripcion);
        anuncioRepository.save(anuncio);

        if (eliminarFotos != null) {
            for (Long fotoId : eliminarFotos) {
                Optional<Foto> fotoOpt = fotoRepository.findById(fotoId);
                if (fotoOpt.isPresent()) {
                    Foto foto = fotoOpt.get();
                    Path rutaArchivo = Paths.get("uploads").resolve(foto.getNombreArchivo());
                    try {
                        Files.deleteIfExists(rutaArchivo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fotoRepository.delete(foto);
                }
            }
        }

        if (fotos != null && fotos.length > 0 && !fotos[0].isEmpty()) {
            Path uploadDir = Paths.get("uploads");
            try {
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("error", "Error al crear directorio para imágenes.");
                return "formulario-anuncio";
            }

            for (MultipartFile foto : fotos) {
                if (!foto.isEmpty() && foto.getContentType() != null && foto.getContentType().startsWith("image")) {
                    try {
                        BufferedImage originalImage = ImageIO.read(foto.getInputStream());
                        if (originalImage == null) {
                            model.addAttribute("error", "Uno de los archivos no es una imagen válida.");
                            return "formulario-anuncio";
                        }

                        int originalWidth = originalImage.getWidth();
                        int originalHeight = originalImage.getHeight();
                        int newWidth = 1000;
                        int newHeight = (int) (((double) originalHeight / originalWidth) * newWidth);

                        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = resizedImage.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                        g2d.dispose();

                        String nombreArchivo = UUID.randomUUID().toString() + "_" + foto.getOriginalFilename();
                        Path rutaArchivo = uploadDir.resolve(nombreArchivo);

                        ImageIO.write(resizedImage, "jpg", rutaArchivo.toFile());

                        Foto nuevaFoto = new Foto();
                        nuevaFoto.setNombreArchivo(nombreArchivo);
                        nuevaFoto.setAnuncio(anuncio);
                        fotoRepository.save(nuevaFoto);

                    } catch (IOException e) {
                        e.printStackTrace();
                        model.addAttribute("error", "Error al procesar la imagen.");
                        return "formulario-anuncio";
                    }
                } else {
                    model.addAttribute("error", "Uno de los archivos no es una imagen válida.");
                    return "formulario-anuncio";
                }
            }
        }

        return "redirect:/";
    }

    @GetMapping("/ver/{id}")
    public String verAnuncio(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Anuncio> anuncioOpt = anuncioRepository.findById(id);

        if (anuncioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El anuncio no existe.");
            return "redirect:/";
        }

        model.addAttribute("anuncio", anuncioOpt.get());
        return "detalle-anuncio";
    }


    @GetMapping("/editar/{id}")
    public String editarAnuncio(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Anuncio> anuncioOpt = anuncioRepository.findById(id);

        if (anuncioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El anuncio no existe.");
            return "redirect:/";
        }

        Anuncio anuncio = anuncioOpt.get();

        if (anuncio.getUsuario() == null) {
            redirectAttributes.addFlashAttribute("error", "Error: Este anuncio no tiene un propietario asignado.");
            return "redirect:/";
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        // Verificar si el usuario autenticado es el dueño del anuncio.
        if (usuario == null || !anuncio.getUsuario().getId().equals(usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", "No puedes editar este anuncio porque no te pertenece.");
            return "redirect:/anuncios";
        }

        model.addAttribute("anuncio", anuncio);
        return "formulario-anuncio";
    }


    @PostMapping("/eliminar/{id}")
    public String eliminarAnuncio(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Anuncio> anuncioOpt = anuncioRepository.findById(id);

        if (anuncioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El anuncio no existe.");
            return "redirect:/";
        }

        Anuncio anuncio = anuncioOpt.get();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        if (usuario == null || anuncio.getUsuario() == null || !anuncio.getUsuario().getId().equals(usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para eliminar este anuncio.");
            return "redirect:/";
        }

        // Eliminar el anuncio
        anuncioRepository.delete(anuncio);
        redirectAttributes.addFlashAttribute("success", "Anuncio eliminado correctamente.");

        return "redirect:/";
    }

    @GetMapping("/mis-anuncios")
    public String misAnuncios(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.findByUsername(userDetails.getUsername());
        if (usuario == null) {
            return "redirect:/login";
        }

        List<Anuncio> misAnuncios = anuncioService.findByUsuario(usuario);
        model.addAttribute("anuncios", misAnuncios);
        model.addAttribute("totalAnuncios", misAnuncios.size());

        return "mis-anuncios";
    }

}
