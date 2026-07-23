function HealthStatusCard({ status, message }) {
    const bannerClass =
        status === "Healthy"
            ? "status-banner status-banner--healthy"
            : status === "Unavailable"
              ? "status-banner status-banner--error"
              : "status-banner status-banner--warning";

    return (
        <section className="hero-card">
            <div className="hero-card__topline">
                <span className="eyebrow">System check</span>
                <span className={bannerClass}>{status}</span>
            </div>

            <h1>Backend Health Status</h1>
            <p>{message}</p>
        </section>
    );
}

export default HealthStatusCard;
