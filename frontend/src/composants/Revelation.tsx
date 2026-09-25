import { useEffect, useRef, useState, type ReactNode } from 'react';

/**
 * Révèle son contenu quand il entre dans l'écran.
 *
 * <p>Un {@code IntersectionObserver} plutôt qu'un écouteur de défilement : le
 * navigateur fait le calcul hors du fil principal, et rien ne s'exécute tant
 * que rien ne bouge. Un écouteur de défilement se déclencherait des dizaines de
 * fois par seconde pour ne rien changer la plupart du temps.</p>
 *
 * <p>L'observation cesse dès la première apparition : une carte révélée n'a pas
 * à disparaître si l'on remonte, et continuer à l'observer coûterait sans
 * raison.</p>
 */
export function Revelation({ children, retard = 0 }: { children: ReactNode; retard?: number }) {
  const element = useRef<HTMLDivElement>(null);
  // Sans IntersectionObserver — navigateur ancien, environnement de test — le
  // contenu est visible dès le premier rendu. Une animation absente vaut mieux
  // qu'un écran vide, et l'état initial le dit mieux qu'un effet.
  const [visible, setVisible] = useState(() => typeof IntersectionObserver === 'undefined');

  useEffect(() => {
    const cible = element.current;
    if (!cible || typeof IntersectionObserver === 'undefined') return;

    const observateur = new IntersectionObserver(
      ([entree]) => {
        if (entree.isIntersecting) {
          setVisible(true);
          observateur.disconnect();
        }
      },
      // La marge négative en bas déclenche la révélation un peu avant que
      // l'élément n'atteigne le bord : il finit d'apparaître au moment où on
      // le regarde, au lieu de commencer à ce moment-là.
      { threshold: 0.08, rootMargin: '0px 0px -40px 0px' },
    );

    observateur.observe(cible);
    return () => observateur.disconnect();
  }, []);

  return (
    <div
      ref={element}
      className="revelation"
      data-visible={visible}
      style={retard ? ({ '--retard': `${retard}ms` } as React.CSSProperties) : undefined}
    >
      {children}
    </div>
  );
}
