package at.drinkly.resource;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import at.drinkly.model.DrinkEntry;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DrinkResource {

    //temp TODO move to db
    List<DrinkEntry> entries = new LinkedList<>();

    @GET
    public List<DrinkEntry> getAll() {
        return DrinkEntry.listAll();
    }

    @GET
    @Path("/{user}")
    public List<DrinkEntry> getByUser(@PathParam("user") String username) {
        return DrinkEntry.list("user.username = ?1", username);
    }

    @Path("/new")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response add(DrinkEntry input) {
        if (input == null || input.user == null || input.user.username == null
                || input.user.username.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("user.username is required").build();
        }

        this.entries.add(input);
        String json = "{\"status\":\"ok\",\"id\":\"" + input.getId() + "\"}";
        return Response.status(Response.Status.ACCEPTED).entity(json).type(MediaType.APPLICATION_JSON).build();
    }
}