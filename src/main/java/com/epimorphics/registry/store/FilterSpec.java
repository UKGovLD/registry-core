/******************************************************************
 * File:        FilterSpec.java
 * Created by:  Dave Reynolds
 * Created on:  12 Jun 2017
 * 
 * (c) Copyright 2017, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.registry.store;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.out.NodeFmtLib;

import com.epimorphics.rdfutil.TypeUtil;
import com.epimorphics.registry.util.Prefixes;
import com.epimorphics.util.EpiException;

/**
 * Represents a filter specification, used in filtering search results or register listing results.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class FilterSpec {
    enum Operator {
        equal, lt, lte, gt, gte
    };
    protected String property;
    protected RDFNode value;
    protected Operator op;
    
    public FilterSpec(String property, String value) {
        this(property, Operator.equal, value);
    }
    
    public FilterSpec(String property, Operator op, String value) {
        this(expandProperty(property), op, expandValue(value));
    }
    
    public FilterSpec(String property, RDFNode value) {
        this(property, Operator.equal, value);
    }
    
    public FilterSpec(String property, Operator op, RDFNode value) {
        this.property = property;
        this.value = value;
        this.op = op;
    }
    
    public static boolean isFilterSpec(String key) {
        return FILTER_SPEC.matcher(key).matches();
    }
    protected static final Pattern FILTER_SPEC = Pattern.compile("(min-|minEx-|max-|maxEx-)?([a-zA-Z][\\w-\\.]*:.*)");
    
    public static FilterSpec filterFor(String key, String value) {
        Matcher m = FILTER_SPEC.matcher(key);
        if (m.matches()) {
            Operator op = Operator.equal;
            String opname = m.group(1);
            if ( opname != null && !opname.isEmpty() ) {
                if ( opname.startsWith("min") )  {
                    op = opname.equals("min-") ? Operator.gte : Operator.gt;
                } else {
                    op = opname.equals("max-") ? Operator.lte : Operator.lt;
                    
                }
            }
            return new FilterSpec(m.group(2), op, value);
        } else {
            throw new EpiException(key + " is not a legal filter spec");
        }
    }
    
    public static String expandProperty(String prop) {
        return Prefixes.get().expandPrefix(prop);
    }
    
    public static RDFNode expandValue(String value) {
        Matcher m = PREFIX.matcher(value);
        if (m.matches() && !TypeUtil.URL_PATTERN.matcher(value).matches()) {
            value = Prefixes.get().expandPrefix(value);
        }
        return TypeUtil.asTypedValue(value);
    }
    
    protected static final Pattern PREFIX = Pattern.compile("([a-zA-Z][\\w-\\.]*):.*");
    
    /**
     * Generate a SPARQL query and filter fragment for this filter
     * @param var  the variable representing the entity being queried
     * @param count the index of this filter in a set of filters
     */
    public String asQuery(String var, int count) {
        StringBuffer buf = new StringBuffer();
        buf.append( String.format("    ?%s <%s> ?var%d .\n", var, property, count) );
        buf.append( String.format("    FILTER(?var%d %s %s)\n", count, operator(), NodeFmtLib.str( value.asNode() ) ) );
        return buf.toString();
    }
    
    @Override
    public String toString() {
        return String.format("filter: <%s> %s %s", property, op.name(), NodeFmtLib.str( value.asNode() ));
    }
    
    protected String operator() {
        switch(op) {
        case equal: return "=";
        case lt:    return "<";
        case lte:   return "<=";
        case gt:    return ">";
        case gte:   return ">=";
        }
        return null;
    }
    
    public static String asQuery(List<FilterSpec> filters, String var) {
        if (filters == null) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        int count = 0;
        for (FilterSpec spec: filters) {
            buf.append( spec.asQuery(var, count) );
            count ++;
        }
        return buf.toString();
    }
}
