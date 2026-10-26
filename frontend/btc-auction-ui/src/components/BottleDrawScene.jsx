import { useEffect, useEffectEvent, useRef } from "react";
import * as THREE from "three";
import { RoomEnvironment } from "three/addons/environments/RoomEnvironment.js";

function BottleDrawScene({ progress, drawing, revealed }) {
    const hostRef = useRef(null);
    const renderFrameRef = useRef(null);
    const readTimeline = useEffectEvent(() => ({ progress, drawing, revealed }));
    const renderCurrentFrame = useEffectEvent(() => {
        renderFrameRef.current?.(readTimeline());
    });

    useEffect(() => {
        const host = hostRef.current;
        let renderer;
        try {
            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
        } catch {
            host.dataset.renderState = "unavailable";
            return undefined;
        }

        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.25));
        renderer.shadowMap.enabled = true;
        renderer.shadowMap.type = THREE.PCFShadowMap;
        renderer.toneMapping = THREE.ACESFilmicToneMapping;
        renderer.toneMappingExposure = 0.85;
        renderer.domElement.setAttribute("aria-label", "Glass draw bottle on a cricket crease with a bat, ball and wickets");
        host.appendChild(renderer.domElement);

        const scene = new THREE.Scene();
        scene.background = new THREE.Color("#eceff1");
        scene.fog = new THREE.Fog("#eceff1", 12, 25);
        const camera = new THREE.PerspectiveCamera(35, 1, 0.1, 50);
        const environmentGenerator = new THREE.PMREMGenerator(renderer);
        const room = new RoomEnvironment();
        const environment = environmentGenerator.fromScene(room, 0.04);
        scene.environment = environment.texture;
        room.dispose();

        scene.add(new THREE.HemisphereLight(0xffffff, 0x727d65, 0.9));
        const keyLight = new THREE.DirectionalLight(0xfff4df, 2.6);
        keyLight.position.set(-3, 7, 5);
        keyLight.castShadow = true;
        keyLight.shadow.mapSize.set(1024, 1024);
        keyLight.shadow.camera.left = -5;
        keyLight.shadow.camera.right = 5;
        keyLight.shadow.camera.top = 7;
        keyLight.shadow.camera.bottom = -5;
        keyLight.shadow.normalBias = 0.04;
        scene.add(keyLight);
        const rimLight = new THREE.DirectionalLight(0x91dce9, 1.4);
        rimLight.position.set(4, 4, -3);
        scene.add(rimLight);

        const mesh = (geometry, material, parent = scene) => {
            const object = new THREE.Mesh(geometry, material);
            object.castShadow = true;
            object.receiveShadow = true;
            parent.add(object);
            return object;
        };
        const groundMaterial = new THREE.MeshStandardMaterial({ color: "#c8b994", roughness: 0.96 });
        const ground = mesh(new THREE.PlaneGeometry(50, 50), groundMaterial);
        ground.rotation.x = -Math.PI / 2;
        ground.position.y = -0.035;
        const pitch = mesh(new THREE.BoxGeometry(3.7, 0.035, 9),
            new THREE.MeshStandardMaterial({ color: "#d8cba9", roughness: 1 }));
        pitch.position.z = -0.5;
        const chalkMaterial = new THREE.MeshStandardMaterial({ color: "#fffaf0", roughness: 1 });
        [-1.5, 1.5].forEach(horizontal => {
            mesh(new THREE.BoxGeometry(0.035, 0.018, 7.5), chalkMaterial).position.set(horizontal, 0.03, -0.5);
        });
        [-1.2, 1.65].forEach(depth => {
            mesh(new THREE.BoxGeometry(5.4, 0.018, 0.045), chalkMaterial).position.set(0, 0.03, depth);
        });

        const bottle = new THREE.Group();
        scene.add(bottle);
        const profile = [
            [0, 0.08], [0.55, 0.08], [0.72, 0.14], [0.79, 0.28],
            [0.8, 0.48], [0.8, 1.65], [0.77, 1.87], [0.68, 2.04],
            [0.43, 2.25], [0.28, 2.43], [0.27, 2.91], [0.31, 2.96],
            [0.31, 3.08], [0.23, 3.08], [0.22, 2.48], [0.36, 2.26],
            [0.64, 1.98], [0.71, 1.76], [0.72, 0.33], [0.57, 0.2], [0, 0.2],
        ].map(([radius, height]) => new THREE.Vector2(radius, height));
        const glassMaterial = new THREE.MeshPhysicalMaterial({
            color: "#e4f6f6", metalness: 0, roughness: 0.07,
            transmission: 0.98, thickness: 0.16, ior: 1.46,
            clearcoat: 1, clearcoatRoughness: 0.05,
            attenuationColor: new THREE.Color("#a4e7e8"), attenuationDistance: 5,
        });
        const glass = mesh(new THREE.LatheGeometry(profile, 96), glassMaterial, bottle);
        glass.castShadow = false;
        glass.receiveShadow = false;
        const lip = mesh(new THREE.TorusGeometry(0.272, 0.043, 12, 64), glassMaterial, bottle);
        lip.rotation.x = Math.PI / 2;
        lip.position.y = 3.035;

        const glassShards = [];
        const shardMaterial = new THREE.MeshPhysicalMaterial({
            color: "#aeeef1", metalness: 0.04, roughness: 0.08,
            transmission: 0.12, thickness: 0.12, ior: 1.46,
            clearcoat: 1, clearcoatRoughness: 0.04,
            transparent: true, opacity: 0.96, depthWrite: false,
            side: THREE.DoubleSide,
        });
        for (let index = 0; index < 26; index += 1) {
            const width = 0.18 + (index % 4) * 0.035;
            const height = 0.23 + (index % 5) * 0.045;
            const shape = new THREE.Shape();
            shape.moveTo(-width * 0.5, -height * 0.42);
            shape.lineTo(-width * 0.08, -height * 0.5);
            shape.lineTo(width * 0.48, -height * 0.22);
            shape.lineTo(width * 0.5, height * 0.16);
            shape.lineTo(width * 0.13, height * 0.5);
            shape.lineTo(-width * 0.12, height * 0.28);
            shape.lineTo(-width * 0.48, height * 0.08);
            shape.closePath();
            const geometry = new THREE.ExtrudeGeometry(shape, {
                depth: 0.032,
                bevelEnabled: true,
                bevelSegments: 2,
                bevelSize: 0.008,
                bevelThickness: 0.006,
                steps: 1,
            });
            const shard = mesh(geometry, shardMaterial.clone());
            const angle = index / 26 * Math.PI * 2;
            const origin = new THREE.Vector3(
                Math.cos(angle) * 0.7,
                0.38 + (index % 7) * 0.22,
                Math.sin(angle) * 0.55,
            );
            const direction = new THREE.Vector3(
                Math.cos(angle) * (0.8 + (index % 3) * 0.12),
                0.45 + (index % 4) * 0.11,
                Math.sin(angle) * (0.8 + (index % 3) * 0.12),
            ).normalize();
            shard.position.copy(origin);
            shard.rotation.set(angle * 0.18, angle, angle * 0.35);
            const initialRotation = shard.rotation.clone();
            shard.visible = false;
            glassShards.push({
                shard,
                origin,
                direction,
                initialRotation,
                rotation: new THREE.Vector3(0.9 + (index % 3) * 0.35, 1.2, 0.65 + (index % 5) * 0.16),
                delay: (index % 5) * 0.006,
            });
        }
        const shatterFlash = new THREE.PointLight(0xb5faff, 0, 4.5, 1.6);
        shatterFlash.position.set(0, 1.2, 0.8);
        scene.add(shatterFlash);

        const cork = mesh(new THREE.CylinderGeometry(0.235, 0.22, 0.34, 32),
            new THREE.MeshStandardMaterial({ color: "#a7794d", roughness: 0.93 }), bottle);
        cork.position.y = 3.17;
        const paperMaterial = new THREE.MeshStandardMaterial({ color: "#fff9e9", roughness: 0.85, side: THREE.DoubleSide });
        const papers = [];
        for (let index = 0; index < 15; index += 1) {
            const angle = index * 2.39996;
            const radius = 0.2 + (index % 3) * 0.13;
            const paper = mesh(new THREE.BoxGeometry(0.36, 0.025, 0.21), paperMaterial, bottle);
            paper.position.set(Math.cos(angle) * radius, 0.28 + index * 0.061, Math.sin(angle) * radius);
            paper.rotation.set(index * 0.12, angle, index * 0.21);
            papers.push({ paper, angle, radius, height: paper.position.y });
        }

        const emergingSlip = mesh(new THREE.PlaneGeometry(0.37, 0.55, 4, 8), paperMaterial, bottle);
        emergingSlip.position.set(0, 2.5, 0.04);
        emergingSlip.rotation.y = 0.2;
        emergingSlip.visible = false;

        const wood = new THREE.MeshStandardMaterial({ color: "#ddbc7d", roughness: 0.65 });
        const wickets = new THREE.Group();
        wickets.position.set(1.55, 0, -1.2);
        scene.add(wickets);
        [-0.17, 0, 0.17].forEach(horizontal => {
            mesh(new THREE.CylinderGeometry(0.037, 0.037, 1.3, 12), wood, wickets).position.set(horizontal, 0.65, 0);
        });
        [-0.09, 0.09].forEach(horizontal => {
            const bail = mesh(new THREE.CylinderGeometry(0.025, 0.025, 0.19, 10), wood, wickets);
            bail.rotation.z = Math.PI / 2;
            bail.position.set(horizontal, 1.32, 0);
        });
        const bat = new THREE.Group();
        bat.position.set(-1.25, 0.1, 0.25);
        bat.rotation.set(-0.25, -0.3, -0.38);
        scene.add(bat);
        const blade = mesh(new THREE.CapsuleGeometry(0.17, 0.85, 8, 16), wood, bat);
        blade.scale.z = 0.32;
        blade.position.y = 0.65;
        const handle = mesh(new THREE.CylinderGeometry(0.049, 0.045, 0.55, 16),
            new THREE.MeshStandardMaterial({ color: "#193f61", roughness: 0.85 }), bat);
        handle.position.y = 1.47;
        for (let index = 0; index < 12; index += 1) {
            const grip = mesh(new THREE.TorusGeometry(0.05, 0.006, 5, 16), handle.material, bat);
            grip.rotation.x = Math.PI / 2;
            grip.position.y = 1.24 + index * 0.038;
        }

        const ball = new THREE.Group();
        ball.position.set(1.07, 0.24, 0.8);
        ball.rotation.set(0.25, 0.2, -0.5);
        scene.add(ball);
        mesh(new THREE.SphereGeometry(0.23, 40, 28),
            new THREE.MeshStandardMaterial({ color: "#a61f36", roughness: 0.37 }), ball);
        [-0.023, 0.023].forEach(offset => {
            const seam = mesh(new THREE.TorusGeometry(0.228, 0.008, 5, 64), chalkMaterial, ball);
            seam.position.z = offset;
        });
        for (let index = 0; index < 36; index += 1) {
            const angle = index / 36 * Math.PI * 2;
            const stitch = mesh(new THREE.BoxGeometry(0.012, 0.02, 0.058), chalkMaterial, ball);
            stitch.position.set(Math.cos(angle) * 0.23, Math.sin(angle) * 0.23, 0);
            stitch.rotation.z = angle;
        }

        let needsRender = true;
        const resize = () => {
            const width = host.clientWidth;
            const height = host.clientHeight;
            if (!width || !height) return;
            renderer.setSize(width, height, false);
            camera.aspect = width / height;
            camera.position.set(4.3, 3.3, camera.aspect < 0.8 ? 11.8 : 10.1);
            camera.lookAt(0, 1.85, 0);
            camera.updateProjectionMatrix();
            needsRender = true;
        };
        const observer = new ResizeObserver(resize);
        observer.observe(host);
        resize();
        const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
        let previousPhase = -1;
        let previousDrawing;
        let previousFrameTime = 0;
        const renderFrame = timeline => {
            const phase = timeline.revealed ? 1 : timeline.progress;
            if (!needsRender && phase === previousPhase && timeline.drawing === previousDrawing) return;
            const frameTime = performance.now();
            if (!needsRender && frameTime - previousFrameTime < 33) return;
            previousFrameTime = frameTime;
            previousPhase = phase;
            previousDrawing = timeline.drawing;
            needsRender = false;
            const mixing = timeline.drawing && !reducedMotion.matches;
            const extraction = THREE.MathUtils.smoothstep(phase, 0.48, 0.94);
            const glassIntact = phase < 0.82;
            glass.visible = glassIntact;
            lip.visible = glassIntact;
            bottle.rotation.z = mixing ? Math.sin(phase * Math.PI * 14) * 0.13 * (1 - extraction) : 0;
            bottle.rotation.y = mixing ? Math.sin(phase * Math.PI * 5) * 0.35 * (1 - extraction) : 0;
            cork.position.y = 3.17 + extraction * 0.9;
            cork.position.x = extraction * 0.72;
            cork.rotation.z = -extraction * 0.9;
            emergingSlip.visible = extraction > 0 && extraction < 0.99;
            emergingSlip.position.y = 2.52 + extraction * 1.3;
            emergingSlip.rotation.z = Math.sin(extraction * Math.PI) * 0.15;
            papers.forEach(({ paper, angle, radius, height }) => {
                const swirl = mixing ? phase * Math.PI * 7 : 0;
                paper.position.set(Math.cos(angle + swirl) * radius, height + (mixing ? Math.sin(phase * 18 + angle) * 0.12 : 0), Math.sin(angle + swirl) * radius);
                paper.rotation.y = angle + swirl;
            });
            glassShards.forEach(({ shard, origin, direction, initialRotation, rotation, delay }) => {
                const shardProgress = THREE.MathUtils.smoothstep(phase, 0.8 + delay, 0.98 + delay);
                shard.visible = phase >= 0.8 + delay;
                shard.position.copy(origin)
                    .addScaledVector(direction, shardProgress * 1.65);
                shard.position.y -= shardProgress * shardProgress * 0.55;
                shard.rotation.set(
                    initialRotation.x + rotation.x * shardProgress,
                    initialRotation.y + rotation.y * shardProgress,
                    initialRotation.z + rotation.z * shardProgress,
                );
                shard.material.opacity = 0.96 - shardProgress * 0.38;
            });
            const flashProgress = Math.max(0, 1 - Math.abs(phase - 0.84) / 0.11);
            shatterFlash.intensity = flashProgress * 3.5;
            renderer.render(scene, camera);
            host.dataset.renderState = "ready";
            host.dataset.phase = phase.toFixed(3);
            host.dataset.glassBroken = String(!glassIntact);
        };
        renderFrameRef.current = renderFrame;
        renderFrame(readTimeline());

        return () => {
            renderFrameRef.current = null;
            observer.disconnect();
            const materials = new Set();
            scene.traverse(object => {
                object.geometry?.dispose();
                if (object.material) materials.add(object.material);
            });
            materials.forEach(material => material.dispose());
            environment.dispose();
            environmentGenerator.dispose();
            renderer.dispose();
            renderer.domElement.remove();
        };
    }, []);

    useEffect(() => {
        if (!drawing) {
            renderCurrentFrame();
            return undefined;
        }

        let frameId;
        let lastFrameTime = 0;
        const animate = frameTime => {
            if (frameTime - lastFrameTime >= 33) {
                lastFrameTime = frameTime;
                renderCurrentFrame();
            }
            frameId = requestAnimationFrame(animate);
        };
        frameId = requestAnimationFrame(animate);
        return () => cancelAnimationFrame(frameId);
    }, [drawing]);

    return <div className="bottle-draw-webgl" ref={hostRef} data-testid="bottle-draw-scene" />;
}

export default BottleDrawScene;