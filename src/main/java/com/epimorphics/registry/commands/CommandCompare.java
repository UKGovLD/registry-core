package com.epimorphics.registry.commands;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.registry.core.*;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.util.NameUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class CommandCompare extends Command {

    @Override
    public ValidationResponse validate() {
        Description register = store.getCurrentVersion(target);
        if (register == null) {
            return new ValidationResponse(NOT_FOUND, "No such register");
        }
        else if (!(register instanceof Register)) {
            return new ValidationResponse(BAD_REQUEST, "Can only register items in a register");
        }

        return ValidationResponse.OK;
    }

    @Override public Response doExecute() {
        Boolean isEdit = parameters.containsKey(Parameters.COMPARE_EDIT);

        Collection<Resource> entities = findEntities();
        if (entities.isEmpty()) {
            throw new WebApiException(Response.Status.BAD_REQUEST, "No entities found");
        }

        Model result = ModelFactory.createDefaultModel();
        entities.forEach(entity -> {
            entity.inModel(result).addProperty(RDF.type, RegistryVocab.Registrable);
        });

        result.add(store.findSimilar(entities, isEdit));

        return returnModel(result, target);
    }
}
