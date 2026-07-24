export default function SignalLibraryTable({
    data,
    onView,
    onEdit,
    onDelete,
}) {
    if (!data.length) {
        return (
            <div className="empty-state">
                No signal libraries found yet. Create the first entry to start
                mapping engineering signals.
            </div>
        );
    }

    return (
        <table className="entity-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Interface</th>
                    <th>LRU</th>
                    <th>Engineering Unit</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {data.map((item) => (
                    <tr key={item.id}>
                        <td>{item.id}</td>
                        <td>
                            <strong>{item.name}</strong>
                            {item.description ? (
                                <p className="helper-text">{item.description}</p>
                            ) : null}
                        </td>
                        <td>{item.interfaceEntity?.name || "—"}</td>
                        <td>{item.lru?.name || "—"}</td>
                        <td>{item.engineeringUnit || "—"}</td>
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
