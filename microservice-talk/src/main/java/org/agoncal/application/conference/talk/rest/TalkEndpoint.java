package org.agoncal.application.conference.talk.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.agoncal.application.conference.commons.constraints.NotEmpty;
import org.agoncal.application.conference.commons.registry.SpeakerMicroService;
import org.agoncal.application.conference.commons.rest.LinkableEndpoint;
import org.agoncal.application.conference.talk.domain.Speaker;
import org.agoncal.application.conference.talk.domain.Talk;
import org.agoncal.application.conference.talk.domain.Talks;
import org.agoncal.application.conference.talk.repository.TalkRepository;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

import static org.agoncal.application.conference.commons.domain.Links.SELF;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */
@Path("/talks")
@Api(description = "Talks REST Endpoint")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TalkEndpoint extends LinkableEndpoint<Talk> {

    // ======================================
    // =          Injection Points          =
    // ======================================

    @Inject
    private TalkRepository talkRepository;

    @Inject @SpeakerMicroService
    private UriBuilder uriSpeaker;

    // ======================================
    // =            Constructors            =
    // ======================================

    public TalkEndpoint() {
        super(TalkEndpoint.class);
    }

    // ======================================
    // =          Business methods          =
    // ======================================

    @POST
    @ApiOperation(value = "Adds a new talk to the conference")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid input")}
    )
    public Response add(@NotNull Talk talk) {
        Talk created = talkRepository.create(talk);
        return Response.created(getURIForSelf(created)).entity(created).build();
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Finds a talk by ID", response = Talk.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid input"),
        @ApiResponse(code = 404, message = "Talk not found")}
    )
    public Response retrieve(@PathParam("id") String id, @Context Request request) {

        Talk talk = talkRepository.findById(id);

        if (talk == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        EntityTag etag = new EntityTag(Integer.toString(talk.hashCode()));
        Response.ResponseBuilder preconditions = request.evaluatePreconditions(etag);

        // cached resource did change -> serve updated content
        if (preconditions == null) {
            talk.addSelfLink(getURIForSelf(talk));
            talk.addCollectionLink(getURIForCollection());

            for(Speaker speaker: talk.getSpeakers()) {
                speaker.addLink(SELF, uriSpeaker.clone().path(speaker.getId()).build());
            }

            preconditions = Response.ok(talk).tag(etag);
        }

        return preconditions.build();
    }

    @GET
    @ApiOperation(value = "Finds all the talks", response = Talk.class, responseContainer = "List")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid input"),
        @ApiResponse(code = 404, message = "Talks not found")}
    )
    public Response allTalks(@DefaultValue("1") @QueryParam("page") @Min(1) Integer pageNumber) {
        List<Talk> allTalks = talkRepository.findAllTalks(pageNumber);

        if (allTalks == null || allTalks.isEmpty())
            return Response.status(Response.Status.NOT_FOUND).build();

        for (Talk talk : allTalks) {
            talk.addSelfLink(getURIForSelf(talk));

            for(Speaker speaker: talk.getSpeakers()) {
                speaker.addLink(SELF, uriSpeaker.clone().path(speaker.getId()).build());
            }
        }

        Talks talks = new Talks(allTalks);
        Integer last = talkRepository.getNumberOfPages();
        talks.addSelfLink(getURIForPage(pageNumber));
        talks.addFirst(getURIForPage(1));
        talks.addLast(getURIForPage(last));
        talks.addNext(getURIForPage(pageNumber < last ? pageNumber + 1 : last));
        talks.addPrevious(getURIForPage(pageNumber == 1 ? 1 : pageNumber - 1));

        return Response.ok(buildEntities(talks)).build();
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Deletes a talk")
    @ApiResponses(value = {
        @ApiResponse(code = 405, message = "Invalid input")}
    )
    public Response remove(@PathParam("id") @NotEmpty String id) {
        talkRepository.delete(id);
        return Response.noContent().build();
    }

    // ======================================
    // =           Private methods          =
    // ======================================

    private GenericEntity<Talks> buildEntities(final Talks talks) {
        return new GenericEntity<Talks>(talks) {
        };
    }
}
