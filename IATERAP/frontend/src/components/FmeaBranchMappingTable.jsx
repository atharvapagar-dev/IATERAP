export default function FmeaBranchMappingTable({
    data,
    onView,
    onEdit,
    onDelete,
}) {
    if (!data.length) {
        return (
            <div className="empty-state">
                No FMEA branch mappings are available yet. Add one to link a
                fault template to its FMEA taxonomy branch.
            </div>
        );
    }

    return (
        <table className="entity-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Branch Code</th>
                    <th>Failure Effect</th>
                    <th>RPN</th>
                    <th>Fault Template</th>
                    <th>LRU</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {data.map((item) => (
                    <tr key={item.id}>
                        <td>{item.id}</td>
                        <td><strong>{item.fmeaBranchCode}</strong></td>
                        <td>{item.failureEffect}</td>
                        <td>{item.riskPriorityNumber ?? "—"}</td>
                        <td>{item.faultTemplate?.name || "—"}</td>
                        <td>{item.lru?.name || "—"}</td>
                        <td>
                            <div className="table-actions">
                                <button onClick={() => onView(item.id)}>
                                    View
                                </button>
                                <button
                                    className="button--secondary"
                                    onClick={() => onEdit(item.id)}
                                >
                                    Edit
                                </button>
                                <button
                                    className="button--danger"
                                    onClick={() => onDelete(item.id)}
                                >
                                    Delete
                                </button>
                            </div>
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}
