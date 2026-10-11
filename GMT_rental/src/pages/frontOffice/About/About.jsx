import "./About.css";

import AboutHero from "../../../components/frontoffice/About/AboutHero";
import AboutStats from "../../../components/frontoffice/About/AboutStats";
import AboutValues from "../../../components/frontoffice/About/AboutValues";
import WhyGmt from "../../../components/frontoffice/About/WhyGmt";
import AboutCTA from "../../../components/frontoffice/About/AboutCTA";

function About() {
  return (
    <main className="about-page">

      <AboutHero />

      <AboutStats />

      <AboutValues />

      <WhyGmt />

      <AboutCTA />

    </main>
  );
}

export default About;