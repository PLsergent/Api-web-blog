package com.company;

import java.net.*;

public class Main {
    public static void main(String args[]) throws Exception {
        System.out.println("hello world");
        Blog blog = new Blog(new URL("https://liris-ktbs01.insa-lyon.fr:8000/blogephem/"));
        for (Article a: blog.iterArticles("billet")) {
            System.out.println(a.getTitle());
            if (a.getTitle().equals("mon nouveau billet modifié")){
                System.out.println("delete");
                a.delete();
            }
        }

        Article a = blog.createArticle("mon nouveau billet", "bla bla bla");
        // boucle pour vérifier que refresh ne fait pas trop de requêtes GET
        for (int i=0; i<10; i+=1) System.out.println(a.getTitle());
        System.out.println("billet créé\n...");
        Thread.sleep(10*1000);

        a.setTitle(a.getTitle() + " modifié");
        System.out.println("billet modifié\n...");
        Thread.sleep(10*1000);

        a.delete();
        System.out.println("billet supprimé");
    }
}
