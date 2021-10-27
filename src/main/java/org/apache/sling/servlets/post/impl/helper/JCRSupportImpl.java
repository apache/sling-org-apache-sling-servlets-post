/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.servlets.post.impl.helper;

import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.VersioningConfiguration;
import org.apache.sling.servlets.post.exceptions.PreconditionViolatedPersistenceException;
import org.apache.sling.servlets.post.exceptions.TemporaryPersistenceException;

public class JCRSupportImpl {

    private boolean isVersionable(final Node node) throws RepositoryException {
        return node.isNodeType(JcrConstants.MIX_VERSIONABLE);
    }

    public boolean isVersionable(final Resource rsrc) throws PersistenceException {
        try {
            final Node node = rsrc.adaptTo(Node.class);
            return node != null && isVersionable(node);
        } catch ( final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re, rsrc.getPath(), null);
        }
    }

    public boolean checkin(final Resource rsrc)
    throws PersistenceException {
        final Node node = rsrc.adaptTo(Node.class);
        if (node != null) {
            try {
                if (node.isCheckedOut() && isVersionable(node)) {
                    node.getSession().getWorkspace().getVersionManager().checkin(node.getPath());
                    return true;
                }
            } catch (final AccessDeniedException e) {
                throw new PreconditionViolatedPersistenceException(e.getMessage(), e, rsrc.getPath(), null);
            } catch (final UnsupportedRepositoryOperationException|InvalidItemStateException|LockException e) { 
                throw new TemporaryPersistenceException(e.getMessage(), e, rsrc.getPath(), null);
            } catch ( final RepositoryException re) {
                throw new PersistenceException(re.getMessage(), re, rsrc.getPath(), null);
            }
        }
        return false;
    }

    private Node findVersionableAncestor(Node node) throws RepositoryException {
        if (isVersionable(node)) {
            return node;
        }
        try {
            node = node.getParent();
            return findVersionableAncestor(node);
        } catch (ItemNotFoundException | AccessDeniedException e ) {
            // top-level or parent not accessible, stop looking for a versionable ancestor
            return null;
        }
    }

    public void checkoutIfNecessary(final Resource resource,
            final List<Modification> changes,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        if (resource != null && versioningConfiguration.isAutoCheckout()) {
            final Node node = resource.adaptTo(Node.class);
            if ( node != null ) {
                try {
                    Node versionableNode = findVersionableAncestor(node);
                    if (versionableNode != null) {
                        if (!versionableNode.isCheckedOut()) {
                            versionableNode.getSession().getWorkspace().getVersionManager().checkout(versionableNode.getPath());
                            changes.add(Modification.onCheckout(versionableNode.getPath()));
                        }
                    }
                } catch (final AccessDeniedException e) {
                    throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
                } catch (final UnsupportedRepositoryOperationException e) { 
                    throw new TemporaryPersistenceException(e.getMessage(),e);
                } catch (final RepositoryException re) {
                    throw new PersistenceException(re.getMessage(), re);
                }
            }
        }
    }

    public boolean isNode(final Resource rsrc) {
        return rsrc.adaptTo(Node.class) != null;
    }

    public boolean isNodeType(final Resource rsrc, final String typeHint) {
        final Node node = rsrc.adaptTo(Node.class);
        if ( node != null ) {
            try {
                return node.isNodeType(typeHint);
            } catch ( final RepositoryException re) {
                // ignore
            }
        }
        return false;
    }

    public Boolean isFileNodeType(final ResourceResolver resolver, final String nodeType) {
        final Session session = resolver.adaptTo(Session.class);
        if ( session != null ) {
            try {
                final NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                final NodeType nt = ntMgr.getNodeType(nodeType);
                return nt.isNodeType(JcrConstants.NT_FILE);
            } catch (RepositoryException e) {
                // assuming type not valid.
                return null;
            }
        }
        return false;
    }

    private PropertyDefinition searchPropertyDefinition(final NodeType nodeType, final String name) {
        if ( nodeType.getPropertyDefinitions() != null ) {
            for(final PropertyDefinition pd : nodeType.getPropertyDefinitions()) {
                if ( pd.getName().equals(name) ) {
                    return pd;
                }
            }
        }
        // SLING-2877:
        // no need to search property definitions of super types, as nodeType.getPropertyDefinitions()
        // already includes those. see javadoc of {@link NodeType#getPropertyDefinitions()}
        return null;
    }

    private PropertyDefinition searchPropertyDefinition(final Node node, final String name)
    throws RepositoryException {
        PropertyDefinition result = searchPropertyDefinition(node.getPrimaryNodeType(), name);
        if ( result == null ) {
            if ( node.getMixinNodeTypes() != null ) {
                for(final NodeType mt : node.getMixinNodeTypes()) {
                    result = this.searchPropertyDefinition(mt, name);
                    if ( result != null ) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public boolean isPropertyProtectedOrNewAutoCreated(final Object n, final String name)
    throws PersistenceException {
        final Node node = (Node)n;
        try {
            final PropertyDefinition pd = this.searchPropertyDefinition(node, name);
            if ( pd != null ) {
                // SLING-2877 (autocreated check is only required for new nodes)
                if ( (node.isNew() && pd.isAutoCreated()) || pd.isProtected() ) {
                    return true;
                }
            }
        } catch ( final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
        return false;
    }

    public boolean isNewNode(final Object node) {
        return ((Node)node).isNew();
    }

    public boolean isPropertyMandatory(final Object node, final String name)
    throws PersistenceException {
        try {
            final Property prop = ((Node)node).getProperty(name);
            return prop.getDefinition().isMandatory();
        } catch (final PathNotFoundException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch ( final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    public boolean isPropertyMultiple(final Object node, final String name)
    throws PersistenceException {
        try {
            final Property prop = ((Node)node).getProperty(name);
            return prop.getDefinition().isMultiple();
        } catch (final PathNotFoundException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch ( final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    public Integer getPropertyType(final Object node, final String name)
    throws PersistenceException {
        try {
            if ( ((Node)node).hasProperty(name) ) {
                return ((Node)node).getProperty(name).getType();
            }
        } catch (final NoSuchNodeTypeException|ConstraintViolationException|PathNotFoundException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch (final VersionException|LockException e) { 
            throw new TemporaryPersistenceException(e.getMessage(),e);
        } catch (final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
        return null;
    }

    private boolean isWeakReference(int propertyType) {
        return propertyType == PropertyType.WEAKREFERENCE;
    }

    /**
     * {@inheritDoc}
     */
    public Modification storeAsReference(
            final Object n,
            final String name,
            final String[] values,
            final int type,
            final boolean multiValued)
    throws PersistenceException {
        try {
            final Node node = (Node)n;
            if (multiValued) {
                Value[] array = ReferenceParser.parse(node.getSession(), values, isWeakReference(type));
                if (array != null) {
                    return Modification.onModified(
                            node.setProperty(name, array).getPath());
                }
            } else {
                if (values.length >= 1) {
                    Value v = ReferenceParser.parse(node.getSession(), values[0], isWeakReference(type));
                    if (v != null) {
                        return Modification.onModified(
                                node.setProperty(name, v).getPath());
                    }
                }
            }
            return null;
        } catch (final NoSuchNodeTypeException|ConstraintViolationException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch (final VersionException|LockException e) { 
            throw new TemporaryPersistenceException(e.getMessage(),e);
        } catch (final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    public boolean hasSession(final ResourceResolver resolver) {
        return resolver.adaptTo(Session.class) != null;
    }

    public void setTypedProperty(final Object n,
            final String name,
            final String[] values,
            final int type,
            final boolean multiValued)
    throws PersistenceException {
        try {
            if (multiValued) {
                ((Node)n).setProperty(name, values, type);
            } else if (values.length >= 1) {
                ((Node)n).setProperty(name, values[0], type);
            }
        } catch (final ValueFormatException|ConstraintViolationException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch (final VersionException|LockException e) { 
            throw new TemporaryPersistenceException(e.getMessage(),e);
        } catch (final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    public Object getNode(final Resource rsrc) {
        return rsrc.adaptTo(Node.class);
    }

    public Object getItem(final Resource rsrc) {
        return rsrc.adaptTo(Item.class);
    }

    public void setPrimaryNodeType(final Object node, final String type)
    throws PersistenceException {
        try {
            ((Node)node).setPrimaryType(type);
        } catch (final NoSuchNodeTypeException|ConstraintViolationException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch (final VersionException|LockException e) { 
            throw new TemporaryPersistenceException(e.getMessage(),e);
        } catch (final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    public void move(Object src, Object dstParent, String name)
    throws PersistenceException {
        try {
            final Session session = ((Item)src).getSession();
            final Item source = ((Item)src);
            final String targetParentPath = ((Node)dstParent).getPath();
            final String targetPath = (targetParentPath.equals("/") ? "" : targetParentPath) + '/' + name;
            session.move(source.getPath(), targetPath);
        } catch (final PathNotFoundException|ConstraintViolationException|ItemExistsException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch (final VersionException|LockException e) { 
            throw new TemporaryPersistenceException(e.getMessage(),e);
        } catch (final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    /**
     * Copy the <code>src</code> item into the <code>dstParent</code> node.
     * The name of the newly created item is set to <code>name</code>.
     *
     * @param src The item to copy to the new location
     * @param dstParent The node into which the <code>src</code> node is to be
     *            copied
     * @param name The name of the newly created item. If this is
     *            <code>null</code> the new item gets the same name as the
     *            <code>src</code> item.
     * @return the name of the newly created object
     * @throws PersistenceException May be thrown in case of any problem copying
     *             the content.
     * @throws PersistenceException in case something fails
     * @see #copy(Node, Node, String)
     * @see #copy(Property, Node, String)
     */
    public String copy(Object src, Object dstParent, String name)
    throws PersistenceException {
        try {
            final Item result;
            if (((Item)src).isNode()) {
                result = copy((Node) src, (Node)dstParent, name);
            } else {
                result = copy((Property) src, (Node)dstParent, name);
            }
            return result.getPath();
        } catch (final NoSuchNodeTypeException|ConstraintViolationException e) {
            throw new PreconditionViolatedPersistenceException(e.getMessage(),e);
        } catch (final VersionException|LockException e) { 
            throw new TemporaryPersistenceException(e.getMessage(),e);
        } catch (final RepositoryException re) {
            throw new PersistenceException(re.getMessage(), re);
        }
    }

    /**
     * Copy the <code>src</code> node into the <code>dstParent</code> node.
     * The name of the newly created node is set to <code>name</code>.
     * <p>
     * This method does a recursive (deep) copy of the subtree rooted at the
     * source node to the destination. Any protected child nodes and and
     * properties are not copied.
     *
     * @param src The node to copy to the new location
     * @param dstParent The node into which the <code>src</code> node is to be
     *            copied
     * @param name The name of the newly created node. If this is
     *            <code>null</code> the new node gets the same name as the
     *            <code>src</code> node.
     * @throws RepositoryException May be thrown in case of any problem copying
     *             the content.
     */
    private Item copy(Node src, Node dstParent, String name)
            throws RepositoryException {

        if(isAncestorOrSameNode(src, dstParent)) {
            throw new RepositoryException(
                    "Cannot copy ancestor " + src.getPath() + " to descendant " + dstParent.getPath());
        }

        // ensure destination name
        if (name == null) {
            name = src.getName();
        }

        // ensure new node creation
        if (dstParent.hasNode(name)) {
            dstParent.getNode(name).remove();
        }

        // create new node
        Node dst = dstParent.addNode(name, src.getPrimaryNodeType().getName());
        for (NodeType mix : src.getMixinNodeTypes()) {
            dst.addMixin(mix.getName());
        }

        // copy the properties
        for (PropertyIterator iter = src.getProperties(); iter.hasNext();) {
            copy(iter.nextProperty(), dst, null);
        }

        // copy the child nodes
        for (NodeIterator iter = src.getNodes(); iter.hasNext();) {
            Node n = iter.nextNode();
            if (!n.getDefinition().isProtected()) {
                copy(n, dst, null);
            }
        }
        return dst;
    }

    /** 
     * determines if the 2 nodes are in ancestor relationship or identical
     * @param src one node
     * @param dest the other node
     * @return true if src is an ancestor node of dest, or if
     *      both are the same node 
     * @throws RepositoryException if something goes wrong
     **/
    public static boolean isAncestorOrSameNode(Node src, Node dest) throws RepositoryException {
        if(src.getPath().equals("/")) {
            return true;
        } else if(src.getPath().equals(dest.getPath())) {
            return true;
        } else if(dest.getPath().startsWith(src.getPath() + "/")) {
            return true;
        }
        return false;
    }

    /**
     * Copy the <code>src</code> property into the <code>dstParent</code>
     * node. The name of the newly created property is set to <code>name</code>.
     * <p>
     * If the source property is protected, this method does nothing.
     *
     * @param src The property to copy to the new location
     * @param dstParent The node into which the <code>src</code> property is
     *            to be copied
     * @param name The name of the newly created property. If this is
     *            <code>null</code> the new property gets the same name as the
     *            <code>src</code> property.
     * @throws RepositoryException May be thrown in case of any problem copying
     *             the content.
     */
    private Item copy(Property src, Node dstParent, String name)
            throws RepositoryException {
        if (!src.getDefinition().isProtected()) {
            if (name == null) {
                name = src.getName();
            }

            // ensure new property creation
            if (dstParent.hasProperty(name)) {
                dstParent.getProperty(name).remove();
            }

            if (src.getDefinition().isMultiple()) {
                return dstParent.setProperty(name, src.getValues());
            }
            return dstParent.setProperty(name, src.getValue());
        }
        return null;
    }
}
