package com.example.final_project.stay;

import com.example.final_project._core.utils.ApiUtil;
import com.example.final_project.company.SessionCompany;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class StayRestController {
    private final StayService stayService;
    private final HttpSession session;


    // 숙소 검색 기능 (이름, 지역, 날짜, 가격, 인원 수 별 검색)
    @GetMapping("/stay")
    public ResponseEntity<?> searchStay(@RequestBody StayRequest.SearchDTO reqDTO) {
        List<StayResponse.SearchListDTO> respDTO = stayService.getSearchStayList(reqDTO);
        return ResponseEntity.ok(new ApiUtil<>(respDTO));
    }

//    @PostMapping("/api/register")
//    public ResponseEntity<?> save(@RequestBody StayRequest.SaveDTO reqDTO){
//        SessionCompany sessionCompany = (SessionCompany) session.getAttribute("sessionCompany");
//
//        StayResponse.Save respDTO = stayService.register(reqDTO,sessionCompany);
//
//        return ResponseEntity.ok(new ApiUtil<>(respDTO));
//    }

//    @GetMapping("/api/modify-form/{stayId}")
//    public ResponseEntity<?> updateForm(@PathVariable Integer stayId){
//
//       StayResponse.UpdateForm respDTO = stayService.updateForm(stayId);
//
//       return ResponseEntity.ok(new ApiUtil<>(respDTO));
//    }

//    @PutMapping("/api/modify/{stayId}")
//    public ResponseEntity<?> update(@PathVariable Integer stayId,@RequestBody StayRequest.UpdateDTO reqDTO){
//        SessionCompany sessionCompany = (SessionCompany) session.getAttribute("sessionCompany");
//
//        stayService.update(stayId,sessionCompany,reqDTO);
//
//        return ResponseEntity.ok(new ApiUtil<>(respDTO));
//    }

    @PutMapping("/api/cancel/{stayId}")
    public ResponseEntity<?> delete(@PathVariable Integer stayId){
        SessionCompany sessionCompany = (SessionCompany) session.getAttribute("sessionCompany");
        StayResponse.Delete respDTO = stayService.delete(stayId,sessionCompany);

        return ResponseEntity.ok().body(new ApiUtil<>(respDTO));
    }
}
