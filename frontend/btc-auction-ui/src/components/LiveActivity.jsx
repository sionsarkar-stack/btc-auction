import { useEffect, useState } from "react";

function LiveActivity() {

    const [events, setEvents] = useState([]);

    useEffect(() => {

        loadEvents();

        const interval =
            setInterval(loadEvents, 5000);

        return () =>
            clearInterval(interval);

    }, []);

    const loadEvents = () => {

        fetch("http://localhost:8080/api/events/recent")
            .then(response => response.json())
            .then(data => setEvents(data))
            .catch(error =>
                console.error(error));
    };

    return (
        <div className="section-card">

            <h2>
                🔴 Live Activity
            </h2>

            {events.length === 0 ? (

                <p>
                    No events yet
                </p>

            ) : (

                events.map(event => (

                    <div
                        key={event.id}
                        className="activity-item"
                    >

                        <strong>
                            {event.eventType}
                        </strong>

                        <br />

                        {event.playerName}

                        {event.captainName &&
                            ` → ${event.captainName}`}

                        {event.amount &&
                            event.amount > 0 &&
                            ` ₹${event.amount}`}

                        <br />

                        <small>
                            {event.details}
                        </small>

                    </div>

                ))
            )}

        </div>
    );
}

export default LiveActivity;