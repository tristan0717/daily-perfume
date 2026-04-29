import { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  const [view, setView] = useState('home'); 
  const [selectedPerfume, setSelectedPerfume] = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const discoveryRef = useRef(null);
  
  const [likedPerfumes, setLikedPerfumes] = useState(() => {
    const saved = localStorage.getItem('daily-perfume-likes');
    return saved ? JSON.parse(saved) : [];
  });

  useEffect(() => {
    localStorage.setItem('daily-perfume-likes', JSON.stringify(likedPerfumes));
  }, [likedPerfumes]);

  const [step, setStep] = useState(1);
  const [preferences, setPreferences] = useState({ season: '', vibe: '', avoid: '' });

  const [messages, setMessages] = useState([
    { 
      role: 'bot', 
      text: '안녕하세요! 당신만의 향기를 찾아드리는 Daily Perfume AI입니다. 어떤 향을 찾으시나요? 아래 추천 질문을 누르거나 직접 입력해 보세요!' 
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const chatEndRef = useRef(null);

  const [hoveredNote, setHoveredNote] = useState(null);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 }); 

  const quickReplies = [
    "🌸 포근한 봄 플로럴 향수",
    "🎁 20대 여성 선물용 향수",
    "🌲 지속력 좋은 묵직한 우디",
    "🍋 여름에 뿌리기 좋은 시트러스"
  ];

  useEffect(() => {
    if ((view === 'chat' || view === 'onboarding') && chatEndRef.current) {
      chatEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isLoading, view]);

  const toggleLike = (e, p) => {
    e.stopPropagation();
    if (likedPerfumes.find(item => item.name === p.name)) {
      setLikedPerfumes(likedPerfumes.filter(item => item.name !== p.name));
    } else {
      setLikedPerfumes([...likedPerfumes, p]);
    }
  };

  const handleCardClick = async (p) => {
    setSelectedPerfume(p);
    try {
      const res = await axios.get(`/api/perfumes/${p.id}/recommendations`);
      setRecommendations(res.data || []);
    } catch (err) { console.error(err); }
  };

  const executeSearch = async (textToSearch) => {
    if (!textToSearch.trim() || isLoading) return;

    setMessages(prev => [...prev, { role: 'user', text: textToSearch }]);
    setIsLoading(true); 
    
    const recentUserMsgs = messages.filter(m => m.role === 'user').slice(-2).map(m => m.text);
    const contextQuery = recentUserMsgs.length > 0 
      ? `이전 대화흐름: [${recentUserMsgs.join(' -> ')}] | 이번 요청: ${textToSearch}` 
      : textToSearch;

    try {
      const res = await axios.get(`/api/perfumes/search?keyword=${encodeURIComponent(contextQuery)}`);
      
      const recData = res.data.recommendations || [];
      const customPerfumeData = res.data.custom_perfume || null;

      setMessages(prev => [...prev, {
        role: 'bot',
        text: recData.length > 0 
          ? `'${textToSearch}'에 대한 결과입니다. 마음에 드는 노트를 클릭해 추가로 탐색해 보세요!` 
          : `앗, 해당하는 향수를 찾지 못했습니다. 다르게 표현해 주시겠어요?`,
        results: recData,
        customPerfume: customPerfumeData 
      }]);
    } catch (err) {
      console.error(err);
      setMessages(prev => [...prev, { role: 'bot', text: '오류가 발생했습니다. 잠시 후 다시 시도해주세요.' }]);
    } finally {
      setIsLoading(false); 
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    executeSearch(inputValue);
    setInputValue(''); 
  };

  const handleOnboardingComplete = (finalAvoidParam) => {
    const finalPrompt = `${preferences.season}에 어울리는 ${preferences.vibe}한 느낌의 향수를 추천해줘.${finalAvoidParam !== '없음' ? ` 단, ${finalAvoidParam}은 제외해줘.` : ''}`;
    
    setPreferences({ season: '', vibe: '', avoid: '' });
    setStep(1); 
    setView('chat');
    
    executeSearch(finalPrompt);
  };

  // ==========================================
  // 1. 홈 화면 
  // ==========================================
  if (view === 'home') {
    return (
      <div className="app-wrapper">
        <nav className="nav">
          <a href="/" className="brand brand--dot">Daily Perfume</a>
          <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
            <span onClick={() => setView('chat')} style={{cursor:'pointer', fontWeight:600}}>AI 채팅 검색</span>
            <span onClick={() => setView('likes')} style={{cursor:'pointer', fontWeight:600}}>❤️ 찜 목록 ({likedPerfumes.length})</span>
          </div>
        </nav>

        <section className="hero--store">
          <div className="hero__inner" style={{ maxWidth: '600px', margin: '0 auto', textAlign: 'center' }}>
            <h1 className="hero__title">AI 향수 소믈리에<br/>나만의 향기 찾기</h1>
            <p className="hero__desc" style={{ marginBottom: '40px' }}>
              간단한 취향 테스트로 완벽한 향수를 발견하거나, AI와 직접 대화해보세요.
            </p>
            
            <div style={{ display: 'flex', gap: '15px', justifyContent: 'center', flexWrap: 'wrap' }}>
              <button 
                className="btn--primary" 
                onClick={() => setView('onboarding')} 
                style={{ background: '#111', color: '#fff', padding: '15px 30px', fontSize: '16px', flex: 1, minWidth: '200px' }}
              >
                🌱 처음이신가요? (테스트)
              </button>
              <button 
                className="btn--primary" 
                onClick={() => setView('chat')} 
                style={{ background: '#fff', color: '#111', border: '2px solid #111', padding: '15px 30px', fontSize: '16px', flex: 1, minWidth: '200px' }}
              >
                💬 바로 AI와 대화하기
              </button>
            </div>
          </div>
        </section>

        <section className="benefits" style={{ marginTop: '60px' }}>
          <div className="benefits__intro">
            <div className="section-deco"><h2 className="get-title">User Benefits</h2></div>
            <p className="get-desc">AI 추천으로 맞춤형 향수 발견</p>
          </div>
          <div className="benefits__row">
            <div className="benefit"><div className="benefit__icon">✈️</div><h3 className="benefit__title">다양성</h3><p className="benefit__desc">다양한 향수를 한 곳에서</p></div>
            <div className="benefit"><div className="benefit__icon">🔥</div><h3 className="benefit__title">편리함</h3><p className="benefit__desc">손쉬운 검색과 추천 기능</p></div>
            <div className="benefit"><div className="benefit__icon">📖</div><h3 className="benefit__title">개인화</h3><p className="benefit__desc">개인 취향에 맞춘 추천</p></div>
          </div>
        </section>
        
        <footer style={{padding:'80px', textAlign:'center', color:'#ccc', borderTop:'1px solid var(--border)'}}>
          &copy; 2026 Daily Perfume. All rights reserved.
        </footer>
      </div>
    );
  }

  // ==========================================
  // 2. 온보딩 화면
  // ==========================================
  if (view === 'onboarding') {
    return (
      <div className="app-wrapper" style={{ display: 'flex', flexDirection: 'column', height: '100vh', backgroundColor: '#fff' }}>
        <nav className="nav" style={{ borderBottom: '1px solid #eee' }}>
          <a href="#" className="brand brand--dot" onClick={() => setView('home')}>Daily Perfume</a>
          <span style={{ fontWeight: 600, cursor: 'pointer', color: '#111' }} onClick={() => setView('home')}>취소하고 홈으로</span>
        </nav>
        <div style={{ flex: 1, display: 'flex', justifyContent: 'center', alignItems: 'center', textAlign: 'center' }}>
          <div style={{ maxWidth: '400px', width: '90%' }}>
            <div style={{ marginBottom: '20px', color: 'var(--primary)', fontWeight: 'bold' }}>Step {step} / 3</div>
            
            {step === 1 && (
              <div>
                <h2 style={{ marginBottom: '30px', color: '#111' }}>어떤 계절에 사용할 예정인가요?</h2>
                {['봄', '여름', '가을', '겨울'].map(s => (
                  <button key={s} className="btn--primary" style={{ width: '100%', marginBottom: '10px', padding: '15px' }} onClick={() => { setPreferences({...preferences, season: s}); setStep(2); }}>{s}</button>
                ))}
              </div>
            )}
            {step === 2 && (
              <div>
                <h2 style={{ marginBottom: '30px', color: '#111' }}>원하는 분위기는 무엇인가요?</h2>
                {['러블리', '시크', '포근함', '섹시함'].map(v => (
                  <button key={v} className="btn--primary" style={{ width: '100%', marginBottom: '10px', padding: '15px' }} onClick={() => { setPreferences({...preferences, vibe: v}); setStep(3); }}>{v}</button>
                ))}
              </div>
            )}
            {step === 3 && (
              <div>
                <h2 style={{ marginBottom: '30px', color: '#111' }}>피하고 싶은 향이 있나요?</h2>
                {['달콤한 향', '무거운 향', '없음'].map(a => (
                  <button key={a} className="btn--primary" style={{ width: '100%', marginBottom: '10px', padding: '15px' }} onClick={() => { 
                    handleOnboardingComplete(a); 
                  }}>{a}</button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  // ==========================================
  // 3. 찜 목록 화면
  // ==========================================
  if (view === 'likes') {
    return (
      <div className="app-wrapper">
        <nav className="nav">
          <a href="#" className="brand brand--dot" onClick={() => setView('home')}>Daily Perfume</a>
          <span style={{ fontWeight: 600, cursor: 'pointer' }} onClick={() => setView('chat')}>Back to Chat</span>
        </nav>
        <div style={{padding:'40px', maxWidth:'800px', margin:'0 auto'}}>
          <h2 style={{borderBottom:'2px solid var(--border)', paddingBottom:'10px', color:'#111'}}>❤️ 내가 찜한 향수</h2>
          {likedPerfumes.length === 0 ? (
            <p style={{color:'#666', marginTop:'20px'}}>아직 찜한 향수가 없습니다. 채팅에서 하트를 눌러보세요!</p>
          ) : (
            <div className="chat-results" style={{display:'flex', flexDirection:'column'}}>
              {likedPerfumes.map(p => (
                <div key={p.id || p.name} className="perf-card chat-perf-card" onClick={() => handleCardClick(p)} style={{ position: 'relative' }}>
                  <div onClick={(e) => toggleLike(e, p)} style={{ position: 'absolute', top: '15px', right: '15px', fontSize: '20px', cursor: 'pointer' }}>❤️</div>
                  <div style={{color:'var(--primary)', fontWeight:700, fontSize:'11px', marginBottom:'4px'}}>{p.brand}</div>
                  <div style={{fontSize:'1.1rem', fontWeight:800, marginBottom:'10px', color:'#111', lineHeight:1.2}}>{p.name}</div>
                  
                  <div className="note-list" style={{display: 'flex', flexDirection: 'column', gap: '5px'}}>
                    {['topNotes', 'middleNotes', 'baseNotes'].map(type => (
                      p[type] && p[type].length > 0 && (
                        <div key={type}>
                          <span style={{fontSize:'10px', fontWeight:'bold', color:'#888', marginRight:'5px'}}>
                            {type === 'topNotes' ? 'TOP' : type === 'middleNotes' ? 'MID' : 'BASE'}:
                          </span>
                          {p[type].map((n, i) => (
                            <span key={i} style={{ display:'inline-block', background:'var(--chip-bg)', color:'var(--primary-700)', padding:'4px 8px', borderRadius:'6px', fontSize:'11px', marginRight:'4px' }}>
                              {n.kor}
                            </span>
                          ))}
                        </div>
                      )
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  // ==========================================
  // 4. 채팅 화면
  // ==========================================
  return (
    <div className="app-wrapper chat-wrapper">
      <style>{`
        @keyframes skeleton-pulse {
          0% { opacity: 1; }
          50% { opacity: 0.4; }
          100% { opacity: 1; }
        }
        .skeleton-box {
          background-color: #e9ecef;
          border-radius: 6px;
          animation: skeleton-pulse 1.5s ease-in-out infinite;
        }
      `}</style>

      <nav className="nav">
        <a href="#" className="brand brand--dot" onClick={() => setView('home')}>Daily Perfume</a>
        <div style={{display: 'flex', gap: '15px'}}>
          <span style={{ fontWeight: 600, cursor: 'pointer' }} onClick={() => setView('likes')}>❤️ 찜한 향수 ({likedPerfumes.length})</span>
          <span style={{ fontWeight: 600, cursor: 'pointer' }} onClick={() => setView('home')}>Back to Home</span>
        </div>
      </nav>

      <main className="chat-container" style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 80px)' }}>
        
        <div className="chat-history" style={{ flex: 1, overflowY: 'auto', paddingBottom: '20px' }}>
          {messages.map((msg, idx) => (
            <div key={idx} className={`chat-msg ${msg.role}`}>
              <div className="chat-bubble">
                {msg.text}
              </div>
              
              {msg.results && msg.results.length > 0 && (
                <div className="chat-results">
                  {msg.results.map((p, i) => {
                    const isLiked = likedPerfumes.find(item => item.name === p.name);

                    return (
                      <div key={p.id || p.name || i} className="perf-card chat-perf-card" onClick={() => handleCardClick(p)} style={{ position: 'relative' }}>
                        <div onClick={(e) => toggleLike(e, p)} style={{ position: 'absolute', top: '15px', right: '15px', fontSize: '20px', cursor: 'pointer' }}>
                          {isLiked ? '❤️' : '🤍'}
                        </div>
                        <div style={{color:'var(--primary)', fontWeight:700, fontSize:'11px', marginBottom:'4px'}}>{p.brand}</div>
                        <div style={{fontSize:'1.1rem', fontWeight:800, marginBottom:'10px', color:'#111', lineHeight:1.2, width: '85%'}}>{p.name}</div>
                        
                        <div className="note-list" style={{display: 'flex', flexDirection: 'column', gap: '5px'}}>
                          {['topNotes', 'middleNotes', 'baseNotes'].map(type => {
                            if (!p[type] || p[type].length === 0) return null;
                            const label = type === 'topNotes' ? 'TOP' : type === 'middleNotes' ? 'MID' : 'BASE';
                            
                            return (
                              <div key={type} style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center'}}>
                                <span style={{fontSize:'10px', fontWeight:'bold', color:'#888', marginRight:'5px'}}>{label}:</span>
                                
                                {p[type].map((n, j) => {
                                  return (
                                    <span 
                                      key={j} 
                                      onMouseEnter={() => setHoveredNote(n.eng)}
                                      onMouseLeave={() => setHoveredNote(null)}
                                      onMouseMove={(e) => setMousePos({ x: e.clientX, y: e.clientY })}
                                      onClick={(e) => { e.stopPropagation(); executeSearch(`"${n.kor}" 노트가 들어간 다른 향수 찾아줘`); }}
                                      style={{ display:'inline-block', background:'var(--chip-bg)', color:'var(--primary-700)', padding:'4px 8px', borderRadius:'6px', fontSize:'11px', marginRight:'4px', marginBottom:'4px', cursor: 'pointer' }}
                                    >
                                      {n.kor}
                                    </span>
                                  );
                                })}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* 🔥 AI 조향사 커스텀 믹스 향수 UI (피라미드 구조 적용) */}
              {msg.customPerfume && (
                <div className="custom-perfume-card" style={{ marginTop: '15px', padding: '20px', backgroundColor: '#fdfbf7', borderRadius: '12px', border: '1px solid #e8e0d5', maxWidth: '80%' }}>
                  <h4 style={{ margin: '0 0 10px 0', color: '#8a6d3b', fontSize: '13px' }}>🧪 AI 조향사의 특별한 믹스 제안</h4>
                  <div style={{ fontSize: '1.2rem', fontWeight: 'bold', marginBottom: '15px', color: '#111' }}>"{msg.customPerfume.name}"</div>
                  
                  {/* 💎 향기 피라미드 스타일 */}
                  <div style={{ backgroundColor: '#fff', padding: '15px', borderRadius: '8px', marginBottom: '15px', border: '1px solid #eee' }}>
                    <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#555', textAlign: 'center', marginBottom: '12px', borderBottom: '1px solid #eee', paddingBottom: '8px' }}>💎 향기 피라미드</div>
                    
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                      <div style={{ display: 'flex', alignItems: 'center' }}>
                        <span style={{ fontSize:'10px', fontWeight:'bold', color:'#888', width: '40px' }}>TOP</span>
                        <div>{msg.customPerfume.topNotes?.map((n, idx) => <span key={idx} style={{ display:'inline-block', background:'#f5f5f5', color:'#333', padding:'4px 8px', borderRadius:'6px', fontSize:'11px', marginRight:'4px' }}>{n}</span>)}</div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center' }}>
                        <span style={{ fontSize:'10px', fontWeight:'bold', color:'#888', width: '40px' }}>MID</span>
                        <div>{msg.customPerfume.middleNotes?.map((n, idx) => <span key={idx} style={{ display:'inline-block', background:'#f5f5f5', color:'#333', padding:'4px 8px', borderRadius:'6px', fontSize:'11px', marginRight:'4px' }}>{n}</span>)}</div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center' }}>
                        <span style={{ fontSize:'10px', fontWeight:'bold', color:'#888', width: '40px' }}>BASE</span>
                        <div>{msg.customPerfume.baseNotes?.map((n, idx) => <span key={idx} style={{ display:'inline-block', background:'#f5f5f5', color:'#333', padding:'4px 8px', borderRadius:'6px', fontSize:'11px', marginRight:'4px' }}>{n}</span>)}</div>
                      </div>
                    </div>
                  </div>

                  <p style={{ margin: 0, fontSize: '13px', color: '#555', fontStyle: 'italic', lineHeight: 1.6 }}>
                    {msg.customPerfume.description}
                  </p>
                </div>
              )}
            </div>
          ))}

          {isLoading && (
            <div className="chat-msg bot">
              <div className="chat-bubble" style={{ marginBottom: '15px', color: '#888', fontStyle: 'italic' }}>
                <span className="loading-dot">✨</span> AI가 취향에 맞는 향수를 고르고 있습니다...
              </div>
              <div className="chat-results">
                {[1, 2, 3].map(num => (
                  <div key={num} className="perf-card chat-perf-card" style={{ padding: '20px', border: '1px solid #eee', borderRadius: '12px', minWidth: '220px' }}>
                    <div className="skeleton-box" style={{ width: '40px', height: '12px', marginBottom: '8px' }}></div>
                    <div className="skeleton-box" style={{ width: '80%', height: '24px', marginBottom: '15px' }}></div>
                    <div style={{ display: 'flex', gap: '6px', flexDirection: 'column' }}>
                      <div style={{ display: 'flex', gap: '5px' }}>
                        <div className="skeleton-box" style={{ width: '30px', height: '18px', borderRadius: '20px' }}></div>
                        <div className="skeleton-box" style={{ width: '50px', height: '18px', borderRadius: '20px' }}></div>
                        <div className="skeleton-box" style={{ width: '45px', height: '18px', borderRadius: '20px' }}></div>
                      </div>
                      <div style={{ display: 'flex', gap: '5px' }}>
                        <div className="skeleton-box" style={{ width: '30px', height: '18px', borderRadius: '20px' }}></div>
                        <div className="skeleton-box" style={{ width: '60px', height: '18px', borderRadius: '20px' }}></div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
          
          <div ref={chatEndRef} />
        </div>

        <div className="chat-input-wrapper" style={{ padding: '0 20px 20px 20px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <div style={{ display: 'flex', gap: '8px', overflowX: 'auto', paddingBottom: '5px' }}>
            {quickReplies.map((reply, idx) => (
              <button 
                key={idx}
                onClick={() => executeSearch(reply)}
                disabled={isLoading}
                style={{
                  padding: '8px 14px', borderRadius: '20px', border: '1px solid #ddd', background: '#fff', 
                  color: '#555', fontSize: '13px', cursor: isLoading ? 'not-allowed' : 'pointer', whiteSpace: 'nowrap',
                  boxShadow: '0 2px 5px rgba(0,0,0,0.05)', transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.target.style.background = '#f5f5f5'}
                onMouseLeave={(e) => e.target.style.background = '#fff'}
              >
                {reply}
              </button>
            ))}
          </div>

          <form className="chat-form" onSubmit={handleSendMessage} style={{ position: 'relative', margin: 0 }}>
            <input 
              type="text" className="chat-input" placeholder="찾으시는 향수나 분위기를 입력해주세요..."
              value={inputValue} onChange={(e) => setInputValue(e.target.value)} disabled={isLoading} autoFocus
            />
            <button type="submit" className="chat-submit" disabled={isLoading}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </button>
          </form>
        </div>
      </main>

      {hoveredNote && (
        <div style={{ position: 'fixed', top: `${mousePos.y + 15}px`, left: `${mousePos.x + 15}px`, backgroundColor: 'white', border: '1px solid #eee', padding: '10px', borderRadius: '12px', boxShadow: '0 8px 16px rgba(0,0,0,0.15)', zIndex: 9999, textAlign: 'center', pointerEvents: 'none' }}>
          <img 
            src={`/note-images/${hoveredNote}.jpg`} alt={hoveredNote} 
            style={{ width: '120px', height: '120px', objectFit: 'cover', borderRadius: '8px', display: 'block' }}
            onError={(e) => {
              const currentSrc = e.target.src;
              if (currentSrc.endsWith('.jpg')) e.target.src = `/note-images/${hoveredNote}.png`;
              else if (currentSrc.endsWith('.png')) e.target.src = `/note-images/${hoveredNote}.jpeg`;
              else e.target.style.display = 'none'; 
            }} 
          />
        </div>
      )}

      {selectedPerfume && (
        <div className="modal-overlay" onClick={() => setSelectedPerfume(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <span style={{color:'var(--primary)', fontWeight:700}}>{selectedPerfume.brand}</span>
            <h2 style={{fontSize:'2.5rem', margin:'10px 0', color:'#111'}}>{selectedPerfume.name}</h2>
            
            {}

            <p style={{lineHeight:1.8, color:'#555', marginBottom: '20px'}}>{selectedPerfume.description || "상세 정보가 로딩 중입니다."}</p>
            
            <div style={{ backgroundColor: '#f9f9f9', padding: '20px', borderRadius: '12px', marginBottom: '20px' }}>
              <h3 style={{ marginTop: 0, borderBottom: '2px solid #ddd', paddingBottom: '10px', fontSize: '18px', color: '#333' }}>💎 향기 피라미드</h3>
              
              <div style={{ marginBottom: '15px' }}>
                <strong style={{ display: 'block', color: '#666', fontSize: '12px', marginBottom: '5px' }}>TOP NOTES (첫인상)</strong>
                {selectedPerfume.topNotes?.length > 0 ? selectedPerfume.topNotes.map((n, i) => <span key={i} style={{ marginRight: '8px', fontWeight: 'bold', color: '#444' }}>{n.kor}</span>) : <span>정보 없음</span>}
              </div>
              <div style={{ marginBottom: '15px' }}>
                <strong style={{ display: 'block', color: '#666', fontSize: '12px', marginBottom: '5px' }}>MIDDLE NOTES (메인 향)</strong>
                {selectedPerfume.middleNotes?.length > 0 ? selectedPerfume.middleNotes.map((n, i) => <span key={i} style={{ marginRight: '8px', fontWeight: 'bold', color: '#444' }}>{n.kor}</span>) : <span>정보 없음</span>}
              </div>
              <div>
                <strong style={{ display: 'block', color: '#666', fontSize: '12px', marginBottom: '5px' }}>BASE NOTES (잔향)</strong>
                {selectedPerfume.baseNotes?.length > 0 ? selectedPerfume.baseNotes.map((n, i) => <span key={i} style={{ marginRight: '8px', fontWeight: 'bold', color: '#444' }}>{n.kor}</span>) : <span>정보 없음</span>}
              </div>
            </div>

            <h4 style={{marginTop:'30px', color:'#111'}}>🌿 비슷한 향수</h4>
            <div style={{display:'flex', gap:'10px', overflowX:'auto'}}>
              {recommendations.map(r => (
                <div key={r.id} className="perf-card" style={{minWidth:'160px', padding:'15px'}} onClick={() => handleCardClick(r)}>
                  <div style={{fontSize:'10px', color:'var(--primary)'}}>{r.brand}</div>
                  <div style={{fontWeight:'bold', fontSize:'13px', color:'#111'}}>{r.name}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;