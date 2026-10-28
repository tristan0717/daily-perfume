package com.reco.recommendation.member;
import com.reco.recommendation.repository.PerfumeRepository;
import com.reco.recommendation.service.PerfumeService;
import com.reco.recommendation.dto.PerfumeResponseDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@RestController @RequestMapping("/api/member/saved")
public class MemberController {
 private final MemberRepository members;private final SavedPerfumeRepository saved;private final PerfumeRepository perfumes;private final PerfumeService service;
 public MemberController(MemberRepository members,SavedPerfumeRepository saved,PerfumeRepository perfumes,PerfumeService service){this.members=members;this.saved=saved;this.perfumes=perfumes;this.service=service;}
 @GetMapping @Transactional(readOnly=true) public List<PerfumeResponseDto> list(Authentication auth){return saved.findByMemberIdOrderByIdDesc(auth.getName()).stream().map(x->service.getPerfume(x.getPerfumeId())).toList();}
 @PutMapping("/{perfumeId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 public void save(Authentication auth,@PathVariable Long perfumeId){lock(auth);if(!perfumes.existsById(perfumeId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);if(!saved.existsByMemberIdAndPerfumeId(auth.getName(),perfumeId))saved.save(new SavedPerfume(auth.getName(),perfumeId));}
 @DeleteMapping("/{perfumeId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 public void remove(Authentication auth,@PathVariable Long perfumeId){lock(auth);saved.deleteByMemberIdAndPerfumeId(auth.getName(),perfumeId);}
 private void lock(Authentication auth){members.lockById(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
}
