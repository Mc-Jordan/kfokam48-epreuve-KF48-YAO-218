import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import EcranFormateur from './ecrans/EcranFormateur';
import EcranEtudiant from './ecrans/EcranEtudiant';
import EcranRelecteur from './ecrans/EcranRelecteur';

/** Les trois écrans exigés par la contrainte F2, et rien d'autre. */
export default function App() {
  return (
    <>
      <nav>
        <NavLink to="/formateur">Formateur</NavLink>{' · '}
        <NavLink to="/etudiant">Étudiant</NavLink>{' · '}
        <NavLink to="/relecteur">Relecteur</NavLink>
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/etudiant" replace />} />
          <Route path="/formateur" element={<EcranFormateur />} />
          <Route path="/etudiant" element={<EcranEtudiant />} />
          <Route path="/relecteur" element={<EcranRelecteur />} />
          <Route path="*" element={<p>Cette page n'existe pas.</p>} />
        </Routes>
      </main>
    </>
  );
}
