import { session } from "../auth/session.js";
import { useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/api";

const steps = [
  { key: "tempoDisponivelSemanal", title: "Quanto tempo você tem por semana?", type: "number", suffix: "horas" },
  { key: "orcamentoInicial", title: "Quanto pretende investir para começar?", type: "number", suffix: "R$" },
  { key: "tipoSocializacao", title: "Como prefere praticar?", options: [["INDIVIDUAL","Sozinho"],["SOCIAL","Com outras pessoas"],["INDIFERENTE","Tanto faz"]] },
  { key: "nivelAtividadeFisicaDesejada", title: "Quanto movimento você procura?", options: [["BAIXO","Leve"],["MODERADO","Moderado"],["ALTO","Intenso"],["INDIFERENTE","Tanto faz"]] },
  { key: "ambientePreferido", title: "Onde você prefere praticar?", options: [["CASA","Em casa"],["AO_AR_LIVRE","Ao ar livre"],["AMBIENTE_FECHADO","Em local fechado"],["INDIFERENTE","Tanto faz"]] },
  { key: "nivelExperiencia", title: "Como você se considera ao começar?", options: [["INICIANTE","Iniciante"],["INTERMEDIARIO","Intermediário"],["AVANCADO","Avançado"]] },
];

export default function Profile() {
  const [step, setStep] = useState(0);
  const [form, setForm] = useState({ tempoDisponivelSemanal: 3, orcamentoInicial: 100 });
  const [erro, setErro] = useState("");
  const navigate = useNavigate();
  const current = steps[step];
  const selected = form[current.key];

  function choose(value) {
    setForm((prev) => ({ ...prev, [current.key]: value }));
    setErro("");
  }

  async function next() {
    if (selected === undefined || selected === "") { setErro("Escolha uma resposta para continuar."); return; }
    if (step < steps.length - 1) { setStep(step + 1); return; }
    try {
      const token = session.getItem("token");
      const userId = session.getItem("userId");
      await api.put(`/users/${userId}/profile`, form, { headers: { Authorization: `Bearer ${token}` } });
      navigate("/recommendations");
    } catch (e) { setErro(e.response?.data?.message || "Não foi possível salvar seu perfil."); }
  }

  return (
    <main style={{minHeight:"100vh",display:"grid",placeItems:"center",background:"#f5f6fa",fontFamily:"Arial",padding:20}}>
      <section style={{width:"min(620px,100%)",background:"white",borderRadius:24,padding:32,boxShadow:"0 16px 50px rgba(0,0,0,.08)"}}>
        <div style={{height:8,background:"#eee",borderRadius:8,overflow:"hidden",marginBottom:28}}><div style={{height:"100%",width:`${((step+1)/steps.length)*100}%`,background:"#222"}} /></div>
        <p style={{color:"#777"}}>Pergunta {step + 1} de {steps.length}</p>
        <h1 style={{fontSize:32,margin:"8px 0 28px"}}>{current.title}</h1>
        {current.type === "number" ? (
          <label style={{display:"block"}}><input type="number" min="0" step="0.5" value={selected} onChange={(e)=>choose(Number(e.target.value))} style={{width:"100%",fontSize:28,padding:18,border:"2px solid #ddd",borderRadius:16,boxSizing:"border-box"}}/><span style={{display:"block",marginTop:8,color:"#777"}}>{current.suffix}</span></label>
        ) : (
          <div style={{display:"grid",gap:12}}>{current.options.map(([value,label])=><button key={value} onClick={()=>choose(value)} style={{padding:18,textAlign:"left",fontSize:18,borderRadius:16,border:selected===value?"2px solid #222":"2px solid #e5e5e5",background:selected===value?"#f0f0f0":"white",cursor:"pointer"}}>{label}</button>)}</div>
        )}
        {erro && <p style={{color:"#b00020"}}>{erro}</p>}
        <div style={{display:"flex",justifyContent:"space-between",marginTop:30}}>
          <button disabled={step===0} onClick={()=>setStep(step-1)} style={{padding:"12px 18px"}}>Voltar</button>
          <button onClick={next} style={{padding:"14px 24px",border:0,borderRadius:14,background:"#222",color:"white",fontWeight:700}}>{step===steps.length-1?"Ver meus hobbies":"Continuar"}</button>
        </div>
      </section>
    </main>
  );
}
