export default function ProtocolTagTable({
    data,
    onView,
    onEdit,
    onDelete,
}) {
    if (!data.length) {
        return (
            <div className="empty-state">
                No protocol tags are available yet. Add one to begin
                documenting the protocol-to-LRU mapping.
            </div>
        );
    }

    return (
        <table className="entity-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Value</th>
                    <th>LRU</th>
                    <th>Description</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {data.map((item) => (
                    <tr key={item.id}>
                        <td>{item.id}</td>
                        <td><strong>{item.name}</strong></td>
                        <td>{item.value}</td>
                        <td>{item.lru?.name || "—"}</td>
                        <td>{item.description || "—"}</td>
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
