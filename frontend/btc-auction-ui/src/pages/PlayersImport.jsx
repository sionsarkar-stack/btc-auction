import { useState } from "react";
import { API_URL } from "../config";

function PlayersImport() {

    const [file, setFile] =
        useState(null);

    const [message, setMessage] =
        useState("");

    const uploadCsv = async () => {

        if (!file) {

            setMessage(
                "Please select a CSV file."
            );

            return;
        }

        try {

            const formData =
                new FormData();

            formData.append(
                "file",
                file
            );

            const response =
                await fetch(
                    `${API_URL}/api/players/import`,
                    {
                        method: "POST",
                        body: formData
                    }
                );

            const result =
                await response.text();

            setMessage(
                result
            );

        } catch (error) {

            console.error(
                error
            );

            setMessage(
                "Import failed."
            );
        }
    };

    return (

        <div className="app-container">

            <div className="page-header">

                <h1 className="page-title">
                    📄 Import Players
                </h1>

            </div>

            <div className="form-card">

                <h2>
                    Upload CSV
                </h2>

                <input
                    type="file"
                    accept=".csv"
                    onChange={(e) =>
                        setFile(
                            e.target.files[0]
                        )
                    }
                />

                <br />
                <br />

                <button
                    className="button"
                    onClick={uploadCsv}
                >
                    Import Players
                </button>

                {message && (

                    <div
                        className={
                            message.includes(
                                "failed"
                            )
                                ? "message-error"
                                : "message-success"
                        }
                        style={{
                            marginTop:
                                "15px"
                        }}
                    >
                        {message}
                    </div>

                )}

            </div>

            <div className="form-card">

                <h2>
                    CSV Format
                </h2>

                <pre>
                    {`name,seed
Virat,A
Rohit,A
Rahul,B
Pant,B
Rinku,C

# Optional per-player override: name,seed,basePrice`}
                </pre>

            </div>

        </div>
    );
}

export default PlayersImport;
