/**
 * <p>
 * Contains all relations used by the odktables API for both simple backup and 
 * synchronization services.
 * </p>
 * 
 * <p>
 * The following diagrams present an overview of the relations and their attributes. 
 * (aggregateIdentifier) in parentheses means that the field is represented by the 
 * entity identifier automatically generated by Aggregate for each entity (in the 
 * datastore this is the _URI field).
 * 
 * See the respective entity (e.g. 
 * {@link org.opendatakit.aggregate.odktables.entity.InternalColumn}, 
 * {@link org.opendatakit.aggregate.odktables.entity.InternalUserTableMapping}, 
 * etc.) for definitions of the attributes.
 * <center>
 * <table>
 * <tr>
 * <td><center><img src="doc-files/Columns.png"/></center></td>
 * <td><center><img src="doc-files/Modifications.png"/></center></td>
 * <td><center><img src="doc-files/Permissions.png"/></center></td>
 * <tr>
 * <td><center><img src="doc-files/Table.png"/></center></td>
 * <td><center><img src="doc-files/TableEntries.png"/></center></td>
 * </tr>
 * <tr>
 * <td><center><img src="doc-files/Users.png"/></center></td>
 * <td><center><img src="doc-files/UserTableMappings.png"/></center></td>
 * </tr>
 * </table>
 * </center>
 * </p>
 *  
 */
package org.opendatakit.aggregate.odktables.relation;

