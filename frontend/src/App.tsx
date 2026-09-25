import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import EcranFormateur from './ecrans/EcranFormateur';
import EcranEtudiant from './ecrans/EcranEtudiant';
import EcranRelecteur from './ecrans/EcranRelecteur';

/**
 * Les trois écrans exigés par la contrainte F2, et rien d'autre.
 *
 * La navigation reste au même endroit sur les trois écrans, et l'onglet courant
 * est marqué par la couleur <em>et</em> par un trait : la couleur seule ne dit
 * rien à qui ne la distingue pas.
 */
export default function App() {
  return (
    <>
      <header className="entete">
        <p className="entete__titre">KFOKAM48 · Présences et relectures</p>
        <nav className="onglets" aria-label="Rôles">
          <NavLink to="/formateur">Formateur</NavLink>
          <NavLink to="/etudiant">Étudiant</NavLink>
          <NavLink to="/relecteur">Relecteur</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/etudiant" replace />} />
          <Route path="/formateur" element={<EcranFormateur />} />
          <Route path="/etudiant" element={<EcranEtudiant />} />
          <Route path="/relecteur" element={<EcranRelecteur />} />
          <Route path="*" element={<p className="vide">Cette page n'existe pas.</p>} />
        </Routes>
      </main>
    </>
  );
}
