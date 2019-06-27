package com.epimorphics.registry.commands;

import com.epimorphics.appbase.webapi.WebApiException;
import com.epimorphics.registry.core.*;
import com.epimorphics.registry.vocab.RegistryVocab;
import com.epimorphics.registry.webapi.Parameters;
import com.epimorphics.util.NameUtils;
import com.epimorphics.vocabs.SKOS;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class CommandCompare extends Command {
    private Register register;

    @Override
    public ValidationResponse validate() {
        Description description = store.getCurrentVersion(target);
        if (description == null) {
            return new ValidationResponse(NOT_FOUND, "No such register");
        }
        else if (!(description instanceof Register)) {
            return new ValidationResponse(BAD_REQUEST, "Can only register items in a register");
        }

        this.register = description.asRegister();
        return ValidationResponse.OK;
    }

    private Double getSimilarityParam() {
        Object value = Registry.get().getConfigExtensions().get("compareSimilarity");
        if (value == null) return null; else {
            try {
                return Double.valueOf(value.toString());
            } catch (NumberFormatException nfe) {
                throw new WebApiException(INTERNAL_SERVER_ERROR, "Similarity configuration is invalid: you must assign a valid number between 0 and 1." + nfe);
            }
        }
    }

    private List<RegisterItem> getRegisterItems() {
        List<RegisterItem> items = new ArrayList<>();
        String parentURI = NameUtils.stripLastSlash(register.getRoot().getURI());
        ResIterator itemsIt = payload.listSubjectsWithProperty(RDF.type, RegistryVocab.RegisterItem);

        if (itemsIt.hasNext()) {
            itemsIt.forEachRemaining(resource -> {
                RegisterItem item = RegisterItem.fromRIRequest(resource, parentURI, true);
                items.add(item);
            });
        } else {
            findEntities().forEach(resource -> {
                RegisterItem item = RegisterItem.fromEntityRequest(resource, parentURI, true);
                items.add(item);
            });
        }

        return items;
    }

    @Override public Response doExecute() {
        Boolean isEdit = parameters.containsKey(Parameters.COMPARE_EDIT);
        Double similarity = getSimilarityParam();

        Collection<RegisterItem> items = getRegisterItems();
        if (items.isEmpty()) {
            throw new WebApiException(Response.Status.BAD_REQUEST, "No entities found");
        }

        Model result = ModelFactory.createDefaultModel();
        Resource root = result.createResource();
        root.addProperty(RDF.type, RegistryVocab.CompareResult);

        items.forEach(item -> {
            Resource itemRes = item.getRoot().inModel(result);
            root.addProperty(RDFS.member, itemRes);

            Description existing = store.getDescription(itemRes.getURI());
            if (existing != null) {
                itemRes.addProperty(SKOS.exactMatch, itemRes.getURI());
            }
        });

        result.add(store.findSimilar(items, isEdit, similarity));

        return returnModel(result, target);
    }
}
