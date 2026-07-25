export default function FaultTemplateTable({
    data,
    onView,
    onEdit,
    onDelete,
}) {
    if (!data.length) {
        return (
            <div className="empty-state">
                No fault templates are available yet. Add one to begin
                documenting fault definitions per LRU.
            </div>
        );
    }

    return (
        <table className="entity-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Fault Code</th>
                    <th>Severity</th>
                    <th>LRU</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {data.map((item) => (
                    <tr key={item.id}>
                        <td>{item.id}</td>
                        <td><strong>{item.name}</strong></td>
                        <td>{item.faultCode}</td>
                        <td>{item.severity}</td>
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
