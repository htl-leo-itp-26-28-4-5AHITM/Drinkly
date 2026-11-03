package at.htl;

import java.net.URI;
import java.sql.Timestamp;
import java.util.List;

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
public class DrinkingResource {

    @GET
    public List<DrinkingRecord> getAll() {
        return DrinkingRecord.listAll();
    }

    @GET
    @Path("/{user}")
    public List<DrinkingRecord> getByUser(@PathParam("user") String username) {
        return DrinkingRecord.list("user.username = ?1", username);
    }

    @POST
    @Transactional
    public Response add(DrinkingRecord input) {
        if (input == null || input.user == null || input.user.username == null
                || input.user.username.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("user.username is required").build();
        }

        // Build a fresh entity so a client-supplied id can't interfere
        DrinkingRecord record = new DrinkingRecord();
        record.user = input.user;
        record.drinkingAmount = input.drinkingAmount;
        record.timestamp = input.timestamp != null
                ? input.timestamp
                : new Timestamp(System.currentTimeMillis());
        record.persist();

        return Response.created(URI.create("/entries/" + record.user.username))
                .entity(record).build();
    }
}
