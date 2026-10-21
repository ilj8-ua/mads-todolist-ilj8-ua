package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.EquipoNotFoundException;
import madstodolist.controller.exception.UsuarioNoAutorizadoException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.EquipoServiceException;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class EquipoController {

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    private UsuarioData comprobarUsuarioLogeado() {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null) {
            throw new UsuarioNoLogeadoException();
        }
        UsuarioData usuario = usuarioService.findById(idUsuarioLogeado);
        if (usuario == null) {
            throw new UsuarioNoLogeadoException();
        }
        return usuario;
    }

    private UsuarioData comprobarAdmin() {
        UsuarioData usuario = comprobarUsuarioLogeado();
        if (!usuario.isAdmin()) {
            throw new UsuarioNoAutorizadoException();
        }
        return usuario;
    }

    @GetMapping("/equipos")
    public String listadoEquipos(Model model) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();
        model.addAttribute("usuario", usuarioLogeado);

        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();
        model.addAttribute("equipos", equipos);
        return "listaEquipos";
    }

    @GetMapping("/equipos/{id}")
    public String detalleEquipo(@PathVariable(value="id") Long idEquipo, Model model) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();
        model.addAttribute("usuario", usuarioLogeado);

        try {
            EquipoData equipo = equipoService.recuperarEquipo(idEquipo);
            List<UsuarioData> usuarios = equipoService.usuariosEquipo(idEquipo);
            boolean esMiembro = equipoService.usuarioEnEquipo(idEquipo, usuarioLogeado.getId());
            model.addAttribute("equipo", equipo);
            model.addAttribute("usuarios", usuarios);
            model.addAttribute("esMiembro", esMiembro);
            return "detalleEquipo";
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @PostMapping("/equipos/{id}/unirse")
    public String unirseAEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();
        try {
            equipoService.añadirUsuarioAEquipo(idEquipo, usuarioLogeado.getId());
            flash.addFlashAttribute("mensaje", "Te has unido al equipo correctamente");
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos/" + idEquipo;
    }

    @PostMapping("/equipos/{id}/salir")
    public String salirDeEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();
        try {
            equipoService.eliminarUsuarioDeEquipo(idEquipo, usuarioLogeado.getId());
            flash.addFlashAttribute("mensaje", "Has abandonado el equipo correctamente");
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos/" + idEquipo;
    }

    @GetMapping("/equipos/nuevo")
    public String formNuevoEquipo(@ModelAttribute EquipoData equipoData, Model model) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();
        model.addAttribute("usuario", usuarioLogeado);
        return "formNuevoEquipo";
    }

    @PostMapping("/equipos/nuevo")
    public String nuevoEquipo(@ModelAttribute EquipoData equipoData, Model model, RedirectAttributes flash) {
        UsuarioData usuarioLogeado = comprobarUsuarioLogeado();
        try {
            equipoService.crearEquipo(equipoData.getNombre());
            flash.addFlashAttribute("mensaje", "Equipo creado correctamente");
            return "redirect:/equipos";
        } catch (EquipoServiceException e) {
            model.addAttribute("usuario", usuarioLogeado);
            model.addAttribute("error", e.getMessage());
            return "formNuevoEquipo";
        }
    }

    @GetMapping("/equipos/{id}/editar")
    public String formEditarEquipo(@PathVariable(value="id") Long idEquipo, Model model) {
        UsuarioData usuarioLogeado = comprobarAdmin();
        model.addAttribute("usuario", usuarioLogeado);

        try {
            EquipoData equipo = equipoService.recuperarEquipo(idEquipo);
            model.addAttribute("equipoData", equipo);
            return "formEditarEquipo";
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
    }

    @PostMapping("/equipos/{id}/editar")
    public String editarEquipo(@PathVariable(value="id") Long idEquipo,
                               @ModelAttribute EquipoData equipoData,
                               Model model,
                               RedirectAttributes flash) {
        UsuarioData usuarioLogeado = comprobarAdmin();
        try {
            equipoService.modificarEquipo(idEquipo, equipoData.getNombre());
            flash.addFlashAttribute("mensaje", "Equipo modificado correctamente");
            return "redirect:/equipos";
        } catch (EquipoServiceException e) {
            model.addAttribute("usuario", usuarioLogeado);
            equipoData.setId(idEquipo);
            model.addAttribute("equipoData", equipoData);
            model.addAttribute("error", e.getMessage());
            return "formEditarEquipo";
        }
    }

    @PostMapping("/equipos/{id}/eliminar")
    public String eliminarEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        comprobarAdmin();
        try {
            equipoService.eliminarEquipo(idEquipo);
            flash.addFlashAttribute("mensaje", "Equipo eliminado correctamente");
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos";
    }
}
