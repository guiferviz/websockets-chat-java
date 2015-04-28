package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

import play.libs.F;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.index;


public class Application extends Controller
{

    private static final Application chatApp = new Application();


    public static Result index()
    {
        return ok(index.render("Pruebas WebSocket Java", "Play! Chat 1.0"));
    }

    public static Result index(String path)
    {
        return ok(index.render("Pruebas WebSocket Java", "Play! Chat 1.0"));
    }

    public static Result checkUsername(String name)
    {
        ObjectNode obj = Json.newObject();
        boolean unique = chatApp.checkUniqueUser(name);
        obj.put(Constants.UNIQUE, unique);

        return ok(obj);
    }

    public static WebSocket<JsonNode> chat(final String name)
    {
        return new WebSocket<JsonNode>()
        {

            // Llamado cuando el WebSocket Handshake ha terminado.
            public void onReady(WebSocket.In<JsonNode> in,
                                WebSocket.Out<JsonNode> out)
            {
                boolean unique = false;

                synchronized (chatApp)
                {
                    unique = chatApp.checkUniqueUser(name);
                    if (unique)
                    {
                        // Lo añadimos a los usuarios del chat.
                        chatApp.add(name, out);
                    }
                    else
                    {
                        // Cerramos la conexión.
                        out.close();
                    }
                }

                if (unique)
                {
                    // Por cada mensaje recibido.
                    in.onMessage(new F.Callback<JsonNode>()
                    {
                        public void invoke(JsonNode json)
                        {
                            String msg = json.get(Constants.MSG).textValue();
                            // No reenviamos mensajes vacíos.
                            if (msg.equals(""))
                            {
                                return;
                            }

                            // Log de mensajes en la consola.
                            System.out.println(msg);
                            System.out.println(" - " + name);

                            // Reenviamos a los demás usuarios el mensaje.
                            ObjectNode obj = Json.newObject();
                            obj.put(Constants.AUTHOR, name);
                            obj.put(Constants.MSG, msg);
                            obj.put(Constants.TYPE, Constants.MSG_TYPE);
                            chatApp.sendAll(obj);
                        }
                    });

                    // Cuando el socket es cerrado.
                    in.onClose(new F.Callback0()
                    {
                        public void invoke()
                        {
                            System.out.println(name + " se ha desconectado");

                            // Lo eliminamos del chat
                            chatApp.remove(name);

                            // Informamos a los demás usuarios que se ha desconectado.
                            ObjectNode leave = Json.newObject();
                            leave.put(Constants.AUTHOR, name);
                            leave.put(Constants.TYPE, Constants.LEAVE_TYPE);
                            chatApp.sendAll(leave);
                        }
                    });

                    // Enviar mensaje de bienvenida.
                    String msg = "¡Bienvenido!";
                    int nUsers = chatApp.getNumberUsers() - 1;
                    msg += nUsers == 1 ? " Hay un usuario más conectado" :
                            nUsers > 1 ? " Hay " + nUsers + " usuarios más conectados" :
                            " No hay más usuarios conectados.";
                    ObjectNode obj = Json.newObject();
                    obj.put(Constants.AUTHOR, Constants.APP_AUTHOR);
                    obj.put(Constants.MSG, msg);
                    obj.put(Constants.TYPE, Constants.MSG_TYPE);
                    out.write(obj);

                    // Avisamos al resto de los usuarios de que se ha conectado.
                    ObjectNode join = Json.newObject();
                    join.put(Constants.AUTHOR, name);
                    join.put(Constants.TYPE, Constants.JOIN_TYPE);
                    chatApp.sendAll(join);
                }
            }

        };
    }


    private Map<String, WebSocket.Out<JsonNode>> members;


    public Application()
    {
        members = new HashMap<>();
    }

    /**
     * Envía un objeto JSON a todos los usuarios conectados al chat.
     *
     * @param msg Objeto a enviar.
     */
    public void sendAll(JsonNode msg)
    {
        for (String name : members.keySet())
        {
            if (!name.equals(msg.get(Constants.AUTHOR).textValue()))
            {
                WebSocket.Out<JsonNode> out = members.get(name);

                out.write(msg);
            }
        }
    }

    /**
     * Comprueba que el nombre sea único, que no haya otro usuario con el mismo
     * nombre.
     *
     * @param name Nombre de usuario a comprobar.
     *
     * @return {@code true} si es único.
     */
    public boolean checkUniqueUser(String name)
    {
        return !members.containsKey(name);
    }

    public int getNumberUsers()
    {
        return members.size();
    }

    public void add(String user, WebSocket.Out<JsonNode> out)
    {
        members.put(user, out);
    }

    public void remove(String user)
    {
        members.remove(user);
    }

}
