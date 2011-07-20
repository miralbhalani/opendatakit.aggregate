package org.opendatakit.aggregate.odktables.commandlogic.synchronize;

import java.util.List;

import org.opendatakit.aggregate.odktables.command.synchronize.DeleteSynchronizedTable;
import org.opendatakit.aggregate.odktables.commandlogic.CommandLogic;
import org.opendatakit.aggregate.odktables.commandresult.CommandResult.FailureReason;
import org.opendatakit.aggregate.odktables.commandresult.synchronize.DeleteSynchronizedTableResult;
import org.opendatakit.aggregate.odktables.entity.InternalColumn;
import org.opendatakit.aggregate.odktables.entity.InternalRow;
import org.opendatakit.aggregate.odktables.entity.InternalTableEntry;
import org.opendatakit.aggregate.odktables.entity.InternalUser;
import org.opendatakit.aggregate.odktables.entity.InternalUserTableMapping;
import org.opendatakit.aggregate.odktables.relation.Columns;
import org.opendatakit.aggregate.odktables.relation.Permissions;
import org.opendatakit.aggregate.odktables.relation.Table;
import org.opendatakit.aggregate.odktables.relation.TableEntries;
import org.opendatakit.aggregate.odktables.relation.UserTableMappings;
import org.opendatakit.aggregate.odktables.relation.Users;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.web.CallingContext;

/**
 * DeleteSynchronizedTableLogic encapsulates the logic necessary to validate and
 * execute a DeleteSynchronizedTable command.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public class DeleteSynchronizedTableLogic extends
        CommandLogic<DeleteSynchronizedTable>
{

    private final DeleteSynchronizedTable deleteSynchronizedTable;

    public DeleteSynchronizedTableLogic(
            DeleteSynchronizedTable deleteSynchronizedTable)
    {
        this.deleteSynchronizedTable = deleteSynchronizedTable;
    }

    @Override
    public DeleteSynchronizedTableResult execute(CallingContext cc)
            throws ODKDatastoreException
    {
        Users users = Users.getInstance(cc);
        UserTableMappings mappings = UserTableMappings.getInstance(cc);
        TableEntries entries = TableEntries.getInstance(cc);
        Columns columns = Columns.getInstance(cc);

        String requestingUserID = deleteSynchronizedTable.getRequestingUserID();
        String aggregateTableIdentifier = deleteSynchronizedTable
                .getAggregateTableIdentifier();

        InternalUser requestUser = users.query()
                .equal(Users.USER_ID, requestingUserID).get();
        InternalUserTableMapping mapping;
        try
        {
            mapping = mappings
                    .query()
                    .equal(UserTableMappings.AGGREGATE_TABLE_IDENTIFIER,
                            aggregateTableIdentifier)
                    .equal(UserTableMappings.AGGREGATE_USER_IDENTIFIER,
                            requestUser.getAggregateIdentifier()).get();
        } catch (ODKDatastoreException e)
        {
            return DeleteSynchronizedTableResult.failure(null,
                    FailureReason.TABLE_DOES_NOT_EXIST);
        }

        if (!requestUser.hasPerm(aggregateTableIdentifier, Permissions.DELETE))
        {
            return DeleteSynchronizedTableResult.failure(mapping.getTableID(),
                    FailureReason.PERMISSION_DENIED);
        }

        Table table = Table.getInstance(aggregateTableIdentifier, cc);
        List<InternalRow> rows = table.query().execute();
        List<InternalColumn> cols = columns
                .query()
                .equal(Columns.AGGREGATE_TABLE_IDENTIFIER,
                        aggregateTableIdentifier).execute();
        List<InternalUserTableMapping> maps = mappings
                .query()
                .equal(UserTableMappings.AGGREGATE_TABLE_IDENTIFIER,
                        aggregateTableIdentifier).execute();
        InternalTableEntry entry = entries.get(aggregateTableIdentifier);

        for (InternalRow row : rows)
            row.delete();
        for (InternalColumn column : cols)
            column.delete();
        for (InternalUserTableMapping map : maps)
            map.delete();
        entry.delete();

        return DeleteSynchronizedTableResult.success();
    }
}