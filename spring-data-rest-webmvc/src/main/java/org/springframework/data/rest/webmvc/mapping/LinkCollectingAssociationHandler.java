/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.mapping;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.rest.core.Path;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.util.Assert;

/**
 * {@link SimpleAssociationHandler} that will collect {@link Link}s for all linkable associations.
 * 
 * @author Oliver Gierke
 * @since 2.1
 */
public class LinkCollectingAssociationHandler implements SimpleAssociationHandler {

	private static final String AMBIGUOUS_ASSOCIATIONS = "Detected multiple association links with same relation type! Disambiguate association %s using @RestResource!";

	private final AssociationValueLinks associationValueLinks;

	private final Path basePath;
	private final Object object;
	private final List<Link> links;

	/**
	 * Creates a new {@link LinkCollectingAssociationHandler} for the given {@link PersistentEntities}, {@link Path} and
	 * {@link AssociationLinks}.
	 * 
	 * @param entityLinks must not be {@literal null}.
	 * @param associationLinks must not be {@literal null}.
	 * @param path must not be {@literal null}.
	 */
	public LinkCollectingAssociationHandler(AssociationValueLinks associationLinks, Path path) {
		this(associationLinks, path, null);
	}

	public LinkCollectingAssociationHandler(AssociationValueLinks associationLinks, Object object) {
		this(associationLinks, null, object);
	}

	private LinkCollectingAssociationHandler(AssociationValueLinks associationLinks, Path path, Object object) {

		// Assert.notNull(entityLinks, "PersistentEntities must not be null!");
		// Assert.notNull(path, "Path must not be null!");
		Assert.notNull(associationLinks, "AssociationLinks must not be null!");

		this.associationValueLinks = associationLinks;
		this.basePath = path;
		this.object = object;

		this.links = new ArrayList<Link>();
	}

	/**
	 * Returns the links collected after the {@link Association} has been traversed.
	 * 
	 * @return the links
	 */
	public List<Link> getLinks() {
		return links;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.SimpleAssociationHandler#doWithAssociation(org.springframework.data.mapping.Association)
	 */
	@Override
	public void doWithAssociation(final Association<? extends PersistentProperty<?>> association) {

		PersistentProperty<?> property = association.getInverse();

		if (!associationValueLinks.isLinkableAssociation(property)) {
			return;
		}

		Links existingLinks = new Links(links);

		if (object != null) {

			BeanWrapper<Object> wrapper = BeanWrapper.create(object, null);
			Object associationValue = wrapper.getProperty(property);

			links.addAll(associationValueLinks.getLinksFor(association, associationValue));
			return;
		}

		for (Link link : associationValueLinks.getLinksFor(association, basePath)) {
			if (existingLinks.hasLink(link.getRel())) {
				throw new MappingException(String.format(AMBIGUOUS_ASSOCIATIONS, property.toString()));
			} else {
				links.add(link);
			}
		}
	}
}
