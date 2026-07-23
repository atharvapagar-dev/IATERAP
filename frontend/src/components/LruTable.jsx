export default function LruTable({ data, onView, onEdit, onDelete }) {
    if (!data.length) {
        return (
            <div className="empty-state">
                No LRUs found yet. Add the first LRU to begin tracking the
                inventory.
            </div>
        );
    }

    return (
        <div className="data-table-wrap">
            <table className="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Subsystem</th>
                        <th>Actions</th>
                    </tr>
                </thead>

                <tbody>
                    {data.map((item) => (
                        <tr key={item.id}>
                            <td>{item.id}</td>
                            <td>{item.name || "Unnamed LRU"}</td>
                            <td>
                                {item.subsystemName ||
                                    item.subsystem?.name ||
                                    "Unassigned"}
                            </td>
                            <td>
                                <div className="data-table__actions">
                                    <button
                                        type="button"
                                        onClick={() => onView(item.id)}
                                    >
                                        View
                                    </button>
                                    <button
                                        type="button"
                                        className="button button--ghost"
                                        onClick={() => onEdit(item.id)}
                                    >
                                        Edit
                                    </button>
                                    <button
                                        type="button"
                                        className="button button--danger"
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
        </div>
    );
}
