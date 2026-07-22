export default function LruTable({
    data,
    onView,
    onEdit,
    onDelete,
}) {
    return (
        <table border="1">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Actions</th>
                </tr>
            </thead>

            <tbody>
                {data.map((item) => (
                    <tr key={item.id}>
                        <td>{item.id}</td>

                        <td>{item.name}</td>

                        <td>
                            <button onClick={() => onView(item.id)}>
                                View
                            </button>

                            <button onClick={() => onEdit(item.id)}>
                                Edit
                            </button>

                            <button onClick={() => onDelete(item.id)}>
                                Delete
                            </button>
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}
