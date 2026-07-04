package edu.ing.unict.springboot.server_springboot_maps.api;

import edu.ing.unict.springboot.server_springboot_maps.model.RouteDetails;
import java.util.concurrent.CompletableFuture;

/**
 * Interfaccia per la facade remota di Google Maps.
 * Definisce il contratto per l'interazione con le API di Google Maps
 * per il recupero dei dettagli del percorso.
 */
public interface GoogleMapsRemoteFacade {

    /**
     * Recupera i dettagli del percorso tra un'origine e una destinazione in modo asincrono.
     * Questa operazione include informazioni sul traffico in tempo reale.
     *
     * @param origin Il punto di partenza del percorso.
     * @param destination Il punto di arrivo del percorso.
     * @return Un CompletableFuture che conterrà i dettagli del percorso (RouteDetails)
     * o un'eccezione in caso di fallimento.
     */
    CompletableFuture<RouteDetails> fetchRouteDetails(String origin, String destination);
}

