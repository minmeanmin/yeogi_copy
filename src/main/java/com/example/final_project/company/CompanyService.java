package com.example.final_project.company;

import com.example.final_project._core.errors.exception.Exception400;
import com.example.final_project._core.errors.exception.Exception404;
import com.example.final_project._core.utils.JwtUtil;
import com.example.final_project.pay.Pay;
import com.example.final_project.pay.PayRepository;
import com.example.final_project.pay.PayResponse;
import com.example.final_project.reservation.Reservation;
import com.example.final_project.reservation.ReservationRepository;
import com.example.final_project.room.Room;
import com.example.final_project.room.RoomRepository;
import com.example.final_project.stay.Stay;
import com.example.final_project.stay.StayRepository;
import com.example.final_project.stay.StayResponse;
import com.example.final_project.stay_image.StayImage;
import com.example.final_project.stay_image.StayImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class CompanyService {

    private final PayRepository payRepository;
    private final StayRepository stayRepository;
    private final RoomRepository roomRepository;
    private final CompanyRepository companyRepository;
    private final StayImageRepository stayImageRepository;
    private final ReservationRepository reservationRepository;


    // JWT - 로그인
    @Transactional
    public String jwtlogin(CompanyRequest.LoginDTO reqDTO) {
        //1. 아이디 체크
        Company sessionUser = companyRepository.findByIdAndPassword(reqDTO.getEmail(), reqDTO.getPassword())
                .orElseThrow(() -> new Exception404("아이디 및 패스워드가 일치하지않습니다"));

        String jwt = JwtUtil.companyCreate(sessionUser);

        return jwt;
    }

    // 로그인
    @Transactional
    public SessionCompany login(CompanyRequest.LoginDTO reqDTO) {
        //1. 아이디 체크
        Company sessionUser = companyRepository.findByIdAndPassword(reqDTO.getEmail(), reqDTO.getPassword())
                .orElseThrow(() -> new Exception404("아이디 및 패스워드가 일치하지않습니다"));

        return new SessionCompany(sessionUser);
    }

    // 회원가입
    @Transactional
    public SessionCompany joinAndLogin(CompanyRequest.JoinDTO reqDTO) {

        Optional<Company> companyOP = Optional.ofNullable(companyRepository.findByEmail(reqDTO.getEmail())
                .orElseThrow(() -> new Exception404("해당 이메일을 찾을 수 없습니다")));

        if (companyOP.isPresent()) {
            throw new Exception400("중복된 이메일입니다");
        }

        //회원가입
        Company joinUser = companyRepository.save(reqDTO.toEntity());

        //로그인
        return new SessionCompany(joinUser);

    }

    // 회원수정
    @Transactional
    public SessionCompany updateCompany(Integer companyId, CompanyRequest.UpdateDTO reqDTO) {
        // 인증처리
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception400("로그인이 필요한 서비스 입니다."));
        System.out.println("결과 값 =====================================" + reqDTO);
        // 수정
        company.updateCompany(reqDTO);

        return new SessionCompany(company);
    }

    // [숙소 관리] 로그인한 기업이 등록한 숙소 조회
    public List<CompanyResponse.CompanyStayListDTO> companyStayList(Integer companyId) {
        List<Stay> stayList = stayRepository.findByCompanyId(companyId);

        List<CompanyResponse.CompanyStayListDTO> respDTO = stayList.stream().map(stay -> {
            List<StayImage> stayImageList = stayImageRepository.findByStayId(stay.getId());

            return new CompanyResponse.CompanyStayListDTO(stay, stayImageList.getFirst());
        }).collect(Collectors.toList());

        return respDTO;
    }

    // [숙소 관리 - 숙소 상세보기] 로그인한 기업이 등록한 특정 숙소 상세보기
    public List<CompanyResponse.CompanyStayDetailDTO> companyStayDetailList(Integer stayId) {
        return roomRepository.findAndCountByStayId(stayId);
    }

    // [숙소 관리 - 숙소 상세보기 - 객실 상세보기] 로그인한 기업이 등록한 특정 숙소의 객실 상세보기
    public List<CompanyResponse.CompanyRoomDetailDTO> companyRoomDetail(Integer stayId, String tier) {
        List<Room> roomList = roomRepository.findByStayIdAndTier(stayId, tier);
        return roomList.stream().map(room -> {
            Pay pay = payRepository.findByRoomId(room.getId(), LocalDate.of(2023, 12, 31));
            return new CompanyResponse.CompanyRoomDetailDTO(room, pay);
        }).collect(Collectors.toList());
    }

    public CompanyResponse.CompanyStayListAndTierDTO companyStayListAndTier(Integer stayId, String tier) {
        Optional<Stay> stayOP = stayRepository.findById(stayId);
        Stay stay = null;
        if (stayOP.isPresent()) {
            stay = stayOP.get();
        }
        StayImage stayImage = stayImageRepository.findByStayId(stayId).getFirst();

        return new CompanyResponse.CompanyStayListAndTierDTO(stay, stayImage, tier);
    }

    // [숙소 관리 - 숙소 상세보기 - 객실 상세보기] 로그인한 기업이 등록한 객실의 예약 상세보기
    public CompanyResponse.CompanyReservationDetailDTO companyReservationDetail(Integer reservationId) {
        Reservation reservation = reservationRepository.findByIdWithRoomAndRoomInformation(reservationId);
        return new CompanyResponse.CompanyReservationDetailDTO(reservation);
    }

    // 기업 수익 전체 조회
    public PayResponse.TotalIncomeDTO findTotalIncome(SessionCompany sessionCompany) {
        Company company = companyRepository.findById(sessionCompany.getId())
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));

        // 전체 수익 가져오기
        PayResponse.TotalIncomeDTO respDTO = payRepository.findTotalIncome(company.getId());

        // 만약 수익이 전혀 없으면 0을 반환
        if (respDTO == null) {
            respDTO = new PayResponse.TotalIncomeDTO(company.getId(), 0L, 0L);
        }

        return respDTO;
    }

    // 숙소 수익 전체 조회
    public List<PayResponse.StayTotalIncomeDTO> findIncomeByStay(SessionCompany sessionCompany) {
        Company company = companyRepository.findById(sessionCompany.getId())
                .orElseThrow(() -> new Exception404("존재 하지 않는 계정입니다"));
        List<Stay> stays = stayRepository.findByCompanyId(company.getId());
        List<PayResponse.StayTotalIncomeDTO> respDTO = new ArrayList<>();

        // 만약 숙소가 없으면
        if (stays == null) {
            throw new Exception404("숙소가 존재 하지 않습니다. 숙소를 먼저 등록 해주세요");
        }

        // 전체 수익 가져오기
        for (Stay stay : stays) {
            // 미리 저장을 한다
            List<PayResponse.StayTotalIncomeDTO> saveDTO = payRepository.findIncomeByStay(company.getId(), stay.getId());

            if (saveDTO.isEmpty()) {
                // 예약 내역이 없는 숙소
                PayResponse.StayTotalIncomeDTO zeroDTO = new PayResponse.StayTotalIncomeDTO(company.getId(), stay.getId(), 0L, 0L);
                respDTO.add(zeroDTO);
            } else {
                // 저장된 결과를 모두 저장
                respDTO.addAll(saveDTO);
            }
        }

        return respDTO;
    }
}
