function HealthStatusCard({ status, message }) {
  return (
    <div
      style={{
        border: "1px solid #ccc",
        borderRadius: "8px",
        padding: "20px",
        maxWidth: "400px",
        margin: "20px auto",
        textAlign: "center",
      }}
    >
      <h2>Backend Health Status</h2>

      <h3>{status}</h3>

      <p>{message}</p>
    </div>
  );
}

export default HealthStatusCard;
