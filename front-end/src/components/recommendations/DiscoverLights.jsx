import { useState } from "react";
import "./DiscoverLights.css";
import { AtmosphereContext } from "./atmosphereContext.js";

const lightLayers = [
    // Camada 1: Mais luzes (90) e maior fator de comprimento (8)
    { count: 90, duration: "46s", delay: "-18s", length: 4 },

    // Camada 2: Mais luzes (70) e maior fator de comprimento (12)
    { count: 70, duration: "31s", delay: "-8s", length: 12 },
].map(({ count, duration, delay, length }) => ({
    duration,
    delay,
    lights: Array.from({ length: count }, (_, index) => {
        // Aumentamos a base da largura (de 0.7 para 2.5) e o multiplicador (de 2.3 para 5.0)
        const width = 0.7 + Math.random() ** 2 * 2.3;

        // Aumentamos o fator base da altura para gerar traços/pontos bem maiores
        const height = length * (2.8 + Math.random() ** 1.6 * 2.5);

        return {
            left: `${((index + Math.random()) / count) * 100}%`,
            top: `${Math.random() * 98 + 1}%`,
            opacity: 0.35 + Math.random() * 0.55, // Um pouco mais brilhantes também
            width: `${width.toFixed(2)}px`,
            height: `${height.toFixed(2)}px`,
        };
    }),
}));

function LightTracks() {
    return (
        <div className="discover-lights-plane">
            {lightLayers.map(({ duration, delay, lights }) => (
                <div
                    key={duration}
                    className="discover-lights-track"
                    style={{ "--fall-duration": duration, "--fall-delay": delay }}
                >
                    {[0, 1].map(copy => (
                        <div className="discover-lights-field" key={copy}>
                            {lights.map((style, index) => (
                                <span
                                    key={index}
                                    className="discover-light"
                                    style={style}
                                />
                            ))}
                        </div>
                    ))}
                </div>
            ))}
        </div>
    );
}

function DiscoverLights({ paused }) {
    return (
        <div className="discover-lights" aria-hidden="true" data-paused={paused}>
            <div className="discover-lights-wall">
                <LightTracks />
            </div>
            <div className="discover-lights-floor">
                <LightTracks />
            </div>
        </div>
    );
}

export default function AppAtmosphere({ children }) {
    const [paused, setPaused] = useState(false);
    const toggleLights = () => setPaused(value => !value);

    return (
        <AtmosphereContext.Provider value={{ paused, toggleLights }}>
            <div className="app-atmosphere">
                <DiscoverLights paused={paused} />
                <div className="atmosphere-controls">
                    <button
                        type="button"
                        className="atmosphere-toggle"
                        onClick={toggleLights}
                    >
                        {paused ? "Retomar luzes" : "Pausar luzes"}
                    </button>
                </div>
                {children}
            </div>
        </AtmosphereContext.Provider>
    );
}
