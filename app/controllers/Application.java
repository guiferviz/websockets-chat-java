package controllers;


import play.mvc.Controller;
import play.mvc.Result;

import views.html.index;


public class Application extends Controller
{

    public static Result index()
    {
        return ok(index.render("Pruebas WebSocket Java", "Play! Chat 1.0"));
    }

    public static Result index(String path)
    {
        return ok(index.render("Pruebas WebSocket Java", "Play! Chat 1.0"));
    }

}
