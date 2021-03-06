/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.catalog.query;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.Pipe;
import org.apache.atlas.catalog.Request;
import org.apache.atlas.catalog.VertexWrapper;
import org.apache.atlas.catalog.definition.ResourceDefinition;
import org.apache.atlas.catalog.exception.ResourceNotFoundException;
import org.apache.atlas.catalog.projection.Projection;
import org.apache.atlas.catalog.projection.ProjectionResult;
import org.apache.atlas.repository.graph.TitanGraphProvider;

import java.util.*;

/**
 * Base Query implementation.
 */
public abstract class BaseQuery implements AtlasQuery {
    protected final QueryExpression queryExpression;
    protected final ResourceDefinition resourceDefinition;
    protected final Request request;

    public BaseQuery(QueryExpression queryExpression, ResourceDefinition resourceDefinition, Request request) {
        this.queryExpression = queryExpression;
        this.resourceDefinition = resourceDefinition;
        this.request = request;
    }

    public Collection<Map<String, Object>> execute() throws ResourceNotFoundException {
        Collection<Map<String, Object>> resultMaps = new ArrayList<>();

        for (Vertex vertex : executeQuery()) {
            resultMaps.add(processPropertyMap(new VertexWrapper(vertex, resourceDefinition)));
        }
        return resultMaps;
    }

    private List<Vertex> executeQuery() {
        GremlinPipeline pipeline = getInitialPipeline().as("root");

        Pipe adapterPipe = queryExpression.asPipe();
        //todo: AlwaysQueryAdapter returns null for pipe
        //todo: Is there a no-op pipe that I could add that wouldn't negatively affect performance
        return adapterPipe == null ?
                pipeline.toList() :
                pipeline.add(adapterPipe).back("root").toList();
    }

    protected abstract GremlinPipeline getInitialPipeline();

    // todo: consider getting
    protected Map<String, Object> processPropertyMap(VertexWrapper vertex) {
        Map<String, Object> propertyMap = vertex.getPropertyMap();
        resourceDefinition.filterProperties(request, propertyMap);
        addHref(propertyMap);

        return request.getCardinality() == Request.Cardinality.INSTANCE ?
                applyProjections(vertex, propertyMap) :
                propertyMap;
    }

    protected void addHref(Map<String, Object> propertyMap) {
        String href = resourceDefinition.resolveHref(propertyMap);
        if (href != null) {
            propertyMap.put("href", href);
        }
    }

    private Map<String, Object> applyProjections(VertexWrapper vertex, Map<String, Object> propertyMap) {
        for (Projection p : resourceDefinition.getProjections().values()) {
            for (ProjectionResult projectionResult : p.values(vertex)) {
                if (p.getCardinality() == Projection.Cardinality.MULTIPLE) {
                    propertyMap.put(projectionResult.getName(), projectionResult.getPropertyMaps());
                } else {
                    for (Map<String, Object> projectionMap : projectionResult.getPropertyMaps()) {
                        propertyMap.put(projectionResult.getName(), projectionMap);
                    }
                }
            }
        }
        return propertyMap;
    }

    protected QueryExpression getQueryExpression() {
        return queryExpression;
    }

    protected ResourceDefinition getResourceDefinition() {
        return resourceDefinition;
    }

    protected Request getRequest() {
        return request;
    }

    //todo: abstract
    // Underlying method is synchronized and caches the graph in a static field
    protected TitanGraph getGraph() {
        return TitanGraphProvider.getGraphInstance();
    }
}
