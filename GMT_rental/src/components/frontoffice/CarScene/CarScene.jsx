import { useEffect, useMemo, useRef, useState } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { useGLTF } from "@react-three/drei";
import { Box3, MeshPhysicalMaterial, Vector3 } from "three";
import "./CarScene.css";

const MODEL_PATH = "/models/mercedes-berline.glb";

function LuxuryCar({ reducedMotion }) {
  const root = useRef(null);
  const { scene } = useGLTF(MODEL_PATH, true, true);
  const colors = useSceneColors();
  const normalizedScene = useMemo(() => {
    const model = scene.clone(true);
    model.traverse((object) => {
      if (!object.isMesh) return;
      object.castShadow = true;
      object.receiveShadow = true;
      const replacePaint = (material) => {
        if (!material || material.name !== "PaletteMaterial001") return material;
        return new MeshPhysicalMaterial({
          name: "GMT premium dark paint",
          map: material.map,
          normalMap: material.normalMap,
          aoMap: material.aoMap,
          roughnessMap: material.roughnessMap,
          metalnessMap: material.metalnessMap,
          color: colors.paint,
          metalness: 0.82,
          roughness: 0.29,
          clearcoat: 0.9,
          clearcoatRoughness: 0.08,
        });
      };
      object.material = Array.isArray(object.material)
        ? object.material.map(replacePaint)
        : replacePaint(object.material);
    });

    model.updateMatrixWorld(true);
    const bounds = new Box3().setFromObject(model);
    const size = bounds.getSize(new Vector3());
    const center = bounds.getCenter(new Vector3());
    const scale = 4.7 / Math.max(size.x, size.z);
    model.scale.setScalar(scale);
    model.position.set(-center.x * scale, -bounds.min.y * scale, -center.z * scale);
    return model;
  }, [scene, colors.paint]);

  useFrame((_, delta) => {
    if (root.current && !reducedMotion) root.current.rotation.y += delta * 0.12;
  });

  return <group ref={root} rotation={[0, 0.52, 0]} position={[0, -0.12, 0]}><primitive object={normalizedScene} /></group>;
}

function StageDetails() {
  const colors = useSceneColors();
  return (
    <group position={[0, -0.12, 0]} rotation={[-Math.PI / 2, 0, 0]}>
      <mesh position={[0, 0, -0.012]} rotation={[0, 0, 0]}>
        <circleGeometry args={[2.8, 96]} />
        <meshBasicMaterial color={colors.accent} transparent opacity={0.035} depthWrite={false} />
      </mesh>
      <mesh>
        <ringGeometry args={[2.35, 2.37, 128]} />
        <meshBasicMaterial color={colors.accent} transparent opacity={0.62} depthWrite={false} />
      </mesh>
      <mesh position={[0, 0, 0.005]}>
        <ringGeometry args={[2.68, 2.7, 128]} />
        <meshBasicMaterial color={colors.accent2} transparent opacity={0.42} depthWrite={false} />
      </mesh>
    </group>
  );
}

function readSceneColors() {
  const styles = getComputedStyle(document.documentElement);
  return {
    accent: styles.getPropertyValue("--accent").trim(),
    accent2: styles.getPropertyValue("--accent-2").trim(),
    fill: styles.getPropertyValue("--scene-fill").trim(),
    paint: styles.getPropertyValue("--scene-paint").trim(),
  };
}

function useSceneColors() {
  const [colors, setColors] = useState(readSceneColors);
  useEffect(() => {
    const root = document.documentElement;
    const update = () => setColors(readSceneColors());
    const observer = new MutationObserver(update);
    observer.observe(root, { attributes: true, attributeFilter: ["data-theme"] });
    return () => observer.disconnect();
  }, []);
  return colors;
}

function CarScene({ fallback }) {
  const [reducedMotion, setReducedMotion] = useState(false);
  const colors = useSceneColors();

  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => setReducedMotion(media.matches);
    update();
    media.addEventListener?.("change", update);
    return () => media.removeEventListener?.("change", update);
  }, []);

  return (
    <div className="car-scene" role="img" aria-label="Berline Mercedes noire en présentation 3D">
      <Canvas
        fallback={fallback}
        dpr={[1, 1.5]}
        camera={{ fov: 30, position: [0, 1.48, 10.4] }}
        gl={{ alpha: true, antialias: true, powerPreference: "high-performance" }}
        onCreated={({ gl }) => {
          gl.setClearColor(0x000000, 0);
        }}
      >
        <ambientLight intensity={1.5} />
        <directionalLight position={[-4, 5, -4]} color={colors.accent} intensity={3.2} />
        <directionalLight position={[4, 3, 4]} color={colors.fill} intensity={2.6} />
        <directionalLight position={[0, 4, 7]} color={colors.accent2} intensity={1.8} />
        <StageDetails />
        <LuxuryCar reducedMotion={reducedMotion} />
      </Canvas>
      <span className="car-scene-caption">
        Modèle 3D : <a href="https://sketchfab.com/3d-models/2008-mercedes-benz-c63-amg-8f541d1d07b545799a20217c555a3948" target="_blank" rel="noreferrer">Ddiaz Design</a>
        {" · "}<a href="https://creativecommons.org/licenses/by-nc-sa/4.0/" target="_blank" rel="noreferrer">CC BY-NC-SA 4.0</a>
      </span>
    </div>
  );
}

useGLTF.preload(MODEL_PATH);

export default CarScene;
