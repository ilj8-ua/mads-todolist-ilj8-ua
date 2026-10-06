package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import madstodolist.controller.exception.UsuarioNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ManagerUserSession managerUserSession;

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado != null) {
            UsuarioData usuarioLogeado = usuarioService.findById(idUsuarioLogeado);
            model.addAttribute("usuario", usuarioLogeado);
        }

        List<UsuarioData> usuarios = usuarioService.allUsuarios();
        model.addAttribute("usuarios", usuarios);
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable(value="id") Long idUsuario, Model model) {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado != null) {
            UsuarioData usuarioLogeado = usuarioService.findById(idUsuarioLogeado);
            model.addAttribute("usuario", usuarioLogeado);
        }

        UsuarioData usuarioConsultado = usuarioService.findById(idUsuario);
        if (usuarioConsultado == null) {
            throw new UsuarioNotFoundException();
        }

        model.addAttribute("usuarioConsultado", usuarioConsultado);
        return "descripcionUsuario";
    }
}
