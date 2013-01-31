/******************************************************************
 * File:        StoreUtil.java
 * Created by:  Dave Reynolds
 * Created on:  31 Jan 2013
 *
 * (c) Copyright 2013, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import java.util.ArrayList;
import java.util.List;

import com.epimorphics.registry.core.Description;
import com.epimorphics.registry.core.Register;
import com.epimorphics.registry.core.RegisterItem;

/**
 * Helper methods for manipulating a registry store. Should work over any StoreAPI implementation.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class StoreUtil {

    /**
     * Fetch all RegisterItems for a particular version of the register. Fetches all items, including NotAccepted items.
     * All retrieved resources will be version-flattened if required.
     * @param register the register to be updated with a list of its members
     * @param withEntity if true then for each member fetched, the associated entity will also be fetched
     */
    public static List<RegisterItem> fetchMembersAt(StoreAPI store, Register register, long time, boolean withEntity) {
        List<RegisterEntryInfo> members = store.listMembers(register);
        List<RegisterItem> results = new ArrayList<RegisterItem>( members.size() );
        for (RegisterEntryInfo member : members) {
            Description d = store.getVersionAt(member.getItemURI(), time);
            if (d != null) {
                results.add( d.asRegisterItem() );
            }
        }
        return results;
    }

}
