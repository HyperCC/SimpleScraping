/*
 * Copyright (c) 2020. Charlie Condorcet Engineering Student
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License 2.0 which is available at
 *  http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 *  which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package cl.ucn.disc.pdis.simplescraper.mainapp;

import cl.ucn.disc.pdis.simplescraper.dbformater.DbInteraction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.Random;

/**
 * Principal App to do scraping.
 *
 * @author Charlie Condorect.
 */
public class App {

    /**
     * The Logger
     */
    private static final Logger log = LoggerFactory.getLogger(App.class);

    /**
     * Principal Main class.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, SQLException {

        /*
        El "cod" o el numero id al final de la url, en primer semestre de 2020, no sobrepasa los 29800.
        Esto ultimo verificado buscando ultimos profesores agregados este semestre con id 29700 aprox.
        */

        /**
         * Auxiliaries.
         */
        int id = 1;
        int canVoids = 0;
        int maxCod = 29800;
        String url = "http://online.ucn.cl/directoriotelefonicoemail/fichaGenerica/?cod=";
        PrintWriter printWriter = new PrintWriter("records.txt", "UTF-8");

        // Random variable to interleave time.
        Random random = new Random();

        /**
         * Data extracted from telephone directory.
         */
        String nombre = "";
        String cargo = "";
        String unidad = "";
        String email = "";
        String telefono = "";
        String oficina = "";
        String direccion = "";

        /**
         * The database instance.
         */
        DbInteraction theDatabase = new DbInteraction();

        log.info("Initialization of scraping..");

        for (int i = 1; i < maxCod; i++) {

            // Build the URL to check the data.
            StringBuilder actualUrl = new StringBuilder();
            actualUrl.append(url).append(i);
            Document document = null;

            // Timeout in server.
            try {
                document = Jsoup.connect(actualUrl.toString()).get();

            } catch (SocketTimeoutException e) {
                log.error("Timeout for http request. Details: {}", e.getMessage());
            }

            // Verify the index value.
            try {
                nombre = document.getElementById("lblNombre").text();
            } catch (NullPointerException e) {
                log.error("Value null for timout recent. Details: {}", e);
                nombre = "";
            }

            if (!nombre.isEmpty()) {

                // Get variables from URL.
                cargo = document.getElementById("lblCargo").text();
                unidad = document.getElementById("lblUnidad").text();
                email = document.getElementById("lblEmail").text();

                telefono = document.getElementById("lblTelefono").text();
                // Formate to fone number.
                if (!telefono.isEmpty()) {
                    telefono = telefono.substring(5, telefono.length());
                }

                oficina = document.getElementById("lblOficina").text();
                direccion = document.getElementById("lblDireccion").text();

                // Concatenation of Functionary data.
                StringBuilder sbFunctionary = new StringBuilder();
                sbFunctionary.append(id).append(",")
                        .append(nombre).append(",")
                        .append(cargo).append(",")
                        .append(unidad).append(",")
                        .append(email).append(",")
                        .append(telefono).append(",")
                        .append(oficina).append(",")
                        .append(direccion);

                log.debug("New identified: {}", sbFunctionary.toString());

                // Add new valid functionary to dabatase.
                boolean notExistInDb = theDatabase.formatToDatabase(nombre, cargo, unidad, email, telefono, oficina, direccion);

                // Check if the new functionary is added. The db and csv must be same.
                if (notExistInDb) {

                    // Add new valid functionary to csv file.
                    printWriter.println(sbFunctionary.toString());

                    // Time to wait not to do DDoS.
                    try {
                        Thread.sleep(1000 + random.nextInt(1000));

                    } catch (InterruptedException e) {
                        log.error("Thread is interrupted either before or during the activity. Details: {}", e.getMessage());
                    }

                    // ID real to csv file.
                    id++;

                } else {
                    log.info("The new functionary is not added.");
                }

            } else {

                // If 10 connections to the server happen in a row with no data returned.
                canVoids++;
                if (canVoids >= 10) {
                    // Time to wait not to do DDoS.
                    try {
                        Thread.sleep(1000 + random.nextInt(1000));

                    } catch (InterruptedException e) {
                        log.error("Thread is interrupted either before or during the activity. Details: {}", e.getMessage());
                    }
                    canVoids = 0;
                }
            }

        }

        // End of record insertion.
        printWriter.close();
        //connectionSource.close();
        log.info("End of insertions.");
        theDatabase.CloseDBConnection();

        /*
        Thanks to
        ORM Lite documentation https://ormlite.com/javadoc/ormlite-core/doc-files/ormlite_2.html#Using
        Save scv in txt http://decodigo.com/java-crear-archivos-de-texto
         */
    }

}
